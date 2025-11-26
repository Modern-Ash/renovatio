package org.shark.renovatio.cobol.ir.parser;

import org.shark.renovatio.cobol.ir.context.CobolTypeMapper;
import org.shark.renovatio.cobol.ir.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Decomposes COBOL control break patterns into reusable architectural components.
 *
 * <p>This class solves the architectural impedance mismatch between COBOL's
 * file-processing paradigm and modern service-oriented architectures by:
 * <ul>
 *   <li>Extracting data access logic into repository-pattern components</li>
 *   <li>Extracting business rules into discrete, testable units</li>
 *   <li>Converting aggregation logic into stream-based collectors</li>
 *   <li>Extracting validation rules into a validation framework</li>
 * </ul>
 *
 * <p>The decomposition allows the business logic to be reused regardless of
 * whether the data comes from files, databases, APIs, or message queues.
 */
public class BusinessLogicDecomposer {

    private static final Logger log = LoggerFactory.getLogger(BusinessLogicDecomposer.class);

    private int ruleCounter = 0;
    private int aggregationCounter = 0;
    private int validationCounter = 0;

    /**
     * Decomposes a COBOL intermediate model with control break patterns
     * into reusable business logic components.
     *
     * @param model The COBOL intermediate model (must have control break patterns detected)
     * @return Decomposed business logic components
     */
    public DecomposedBusinessLogic decompose(CobolIntermediateModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }

        resetCounters();

        // Extract data access components
        List<DecomposedBusinessLogic.DataAccessComponent> dataAccess =
                extractDataAccessComponents(model);

        // Extract business rules from statements
        List<DecomposedBusinessLogic.BusinessRuleComponent> businessRules =
                extractBusinessRules(model);

        // Extract aggregation logic from control break patterns
        List<DecomposedBusinessLogic.AggregationComponent> aggregations =
                extractAggregations(model);

        // Extract validation rules
        List<DecomposedBusinessLogic.ValidationComponent> validations =
                extractValidations(model);

        // Build metadata
        Map<String, Object> metadata = buildMetadata(model);

        log.info("Decomposed program {} into {} data access, {} business rules, {} aggregations, {} validations",
                model.getProgramId(),
                dataAccess.size(),
                businessRules.size(),
                aggregations.size(),
                validations.size());

        return DecomposedBusinessLogic.builder()
                .programId(model.getProgramId())
                .dataAccessComponents(dataAccess)
                .businessRules(businessRules)
                .aggregations(aggregations)
                .validations(validations)
                .metadata(metadata)
                .build();
    }

    private void resetCounters() {
        ruleCounter = 0;
        aggregationCounter = 0;
        validationCounter = 0;
    }

    /**
     * Extracts data access components from file operations.
     */
    private List<DecomposedBusinessLogic.DataAccessComponent> extractDataAccessComponents(
            CobolIntermediateModel model
    ) {
        List<DecomposedBusinessLogic.DataAccessComponent> components = new ArrayList<>();

        // Group file operations by file name
        Map<String, Set<FileOperationStatement.OperationType>> fileOperations = new LinkedHashMap<>();

        for (CobolParagraph paragraph : model.getParagraphs().values()) {
            for (CobolStatement stmt : paragraph.statements()) {
                if (stmt instanceof FileOperationStatement fop) {
                    fileOperations.computeIfAbsent(fop.fileName(), k -> new LinkedHashSet<>())
                            .add(fop.operationType());
                }
            }
        }

        // Create a data access component for each file
        for (Map.Entry<String, Set<FileOperationStatement.OperationType>> entry : fileOperations.entrySet()) {
            String fileName = entry.getKey();
            Set<FileOperationStatement.OperationType> ops = entry.getValue();

            // Determine access pattern
            DecomposedBusinessLogic.DataAccessComponent.AccessPattern pattern =
                    determineAccessPattern(ops);

            // Create field mappings from data items
            List<DecomposedBusinessLogic.FieldMapping> fieldMappings =
                    createFieldMappings(model.getDataItems(), fileName);

            // Find potential key fields
            List<String> keyFields = findKeyFields(model.getDataItems());

            components.add(new DecomposedBusinessLogic.DataAccessComponent(
                    "DA-" + fileName,
                    toEntityName(fileName),
                    fileName,
                    keyFields,
                    fieldMappings,
                    pattern
            ));
        }

        return components;
    }

    /**
     * Determines the access pattern based on file operations used.
     */
    private DecomposedBusinessLogic.DataAccessComponent.AccessPattern determineAccessPattern(
            Set<FileOperationStatement.OperationType> operations
    ) {
        boolean hasRead = operations.contains(FileOperationStatement.OperationType.READ);
        boolean hasWrite = operations.contains(FileOperationStatement.OperationType.WRITE);
        boolean hasRewrite = operations.contains(FileOperationStatement.OperationType.REWRITE);
        boolean hasDelete = operations.contains(FileOperationStatement.OperationType.DELETE);

        if (hasWrite || hasRewrite || hasDelete) {
            return DecomposedBusinessLogic.DataAccessComponent.AccessPattern.CRUD;
        }
        if (hasRead) {
            return DecomposedBusinessLogic.DataAccessComponent.AccessPattern.READ_SEQUENTIAL;
        }
        return DecomposedBusinessLogic.DataAccessComponent.AccessPattern.READ_ALL;
    }

    /**
     * Creates field mappings from COBOL data items.
     */
    private List<DecomposedBusinessLogic.FieldMapping> createFieldMappings(
            List<CobolDataItem> dataItems,
            String fileName
    ) {
        return dataItems.stream()
                .filter(item -> item.getLevel() > 1) // Skip group levels
                .map(item -> new DecomposedBusinessLogic.FieldMapping(
                        item.getName(),
                        toCamelCase(item.getName()),
                        item.getPicture(),
                        item.getJavaType() != null ? item.getJavaType() : CobolTypeMapper.picToJavaType(item.getPicture()),
                        null // No transformation by default
                ))
                .collect(Collectors.toList());
    }

    /**
     * Finds potential key fields based on naming conventions.
     */
    private List<String> findKeyFields(List<CobolDataItem> dataItems) {
        return dataItems.stream()
                .filter(item -> {
                    String name = item.getName().toUpperCase();
                    return name.contains("KEY") || name.contains("ID") ||
                           name.contains("CODE") || name.contains("CODIGO") ||
                           name.contains("CLAVE") || name.endsWith("-NR") ||
                           name.endsWith("-NO") || name.endsWith("-NUM");
                })
                .map(CobolDataItem::getName)
                .collect(Collectors.toList());
    }

    /**
     * Extracts business rules from COBOL statements.
     */
    private List<DecomposedBusinessLogic.BusinessRuleComponent> extractBusinessRules(
            CobolIntermediateModel model
    ) {
        List<DecomposedBusinessLogic.BusinessRuleComponent> rules = new ArrayList<>();

        for (CobolParagraph paragraph : model.getParagraphs().values()) {
            rules.addAll(extractRulesFromParagraph(paragraph));
        }

        // Also extract from control break patterns
        for (ControlBreakPattern pattern : model.getControlBreakPatterns()) {
            rules.addAll(extractRulesFromControlBreak(pattern));
        }

        return rules;
    }

    /**
     * Extracts business rules from a single paragraph.
     */
    private List<DecomposedBusinessLogic.BusinessRuleComponent> extractRulesFromParagraph(
            CobolParagraph paragraph
    ) {
        List<DecomposedBusinessLogic.BusinessRuleComponent> rules = new ArrayList<>();

        for (CobolStatement stmt : paragraph.statements()) {
            DecomposedBusinessLogic.BusinessRuleComponent rule = convertToBusinessRule(stmt, paragraph.name());
            if (rule != null) {
                rules.add(rule);
            }
        }

        return rules;
    }

    /**
     * Converts a COBOL statement to a business rule component.
     */
    private DecomposedBusinessLogic.BusinessRuleComponent convertToBusinessRule(
            CobolStatement stmt,
            String context
    ) {
        if (stmt instanceof ComputeStatement compute) {
            return createCalculationRule(compute, context);
        }
        if (stmt instanceof MoveStatement move) {
            return createAssignmentRule(move, context);
        }
        if (stmt instanceof IfStatement ifStmt) {
            return createConditionalRule(ifStmt, context);
        }
        if (stmt instanceof EvaluateStatement eval) {
            return createEvaluateRule(eval, context);
        }
        return null;
    }

    /**
     * Creates a calculation business rule from a COMPUTE statement.
     */
    private DecomposedBusinessLogic.BusinessRuleComponent createCalculationRule(
            ComputeStatement compute,
            String context
    ) {
        String ruleId = "RULE-CALC-" + (++ruleCounter);

        // Extract input fields from expression
        List<String> inputFields = extractFieldsFromExpression(compute.expression());

        return new DecomposedBusinessLogic.BusinessRuleComponent(
                ruleId,
                "Calculate" + toPascalCase(compute.target()),
                "Calculation: " + compute.target() + " = " + compute.expression(),
                DecomposedBusinessLogic.BusinessRuleComponent.RuleType.CALCULATION,
                inputFields,
                List.of(compute.target()),
                compute.expression(),
                List.of(compute)
        );
    }

    /**
     * Creates an assignment business rule from a MOVE statement.
     */
    private DecomposedBusinessLogic.BusinessRuleComponent createAssignmentRule(
            MoveStatement move,
            String context
    ) {
        // Skip simple value saves (MOVE X TO SAVE-X)
        if (isSaveOperation(move)) {
            return null;
        }

        String ruleId = "RULE-ASSIGN-" + (++ruleCounter);

        return new DecomposedBusinessLogic.BusinessRuleComponent(
                ruleId,
                "Assign" + toPascalCase(move.target()),
                "Assignment: " + move.target() + " from " + move.source(),
                DecomposedBusinessLogic.BusinessRuleComponent.RuleType.ASSIGNMENT,
                List.of(move.source()),
                List.of(move.target()),
                move.source() + " -> " + move.target(),
                List.of(move)
        );
    }

    /**
     * Creates a conditional business rule from an IF statement.
     */
    private DecomposedBusinessLogic.BusinessRuleComponent createConditionalRule(
            IfStatement ifStmt,
            String context
    ) {
        String ruleId = "RULE-COND-" + (++ruleCounter);

        List<String> inputFields = extractFieldsFromExpression(ifStmt.condition());
        List<String> outputFields = new ArrayList<>();

        // Collect output fields from then/else branches
        for (CobolStatement thenStmt : ifStmt.thenStatements()) {
            outputFields.addAll(extractOutputFields(thenStmt));
        }
        for (CobolStatement elseStmt : ifStmt.elseStatements()) {
            outputFields.addAll(extractOutputFields(elseStmt));
        }

        return new DecomposedBusinessLogic.BusinessRuleComponent(
                ruleId,
                "Evaluate" + toPascalCase(context) + "Condition",
                "Conditional logic: IF " + ifStmt.condition(),
                DecomposedBusinessLogic.BusinessRuleComponent.RuleType.CONDITIONAL,
                inputFields.stream().distinct().collect(Collectors.toList()),
                outputFields.stream().distinct().collect(Collectors.toList()),
                ifStmt.condition(),
                List.of(ifStmt)
        );
    }

    /**
     * Creates a business rule from an EVALUATE statement.
     */
    private DecomposedBusinessLogic.BusinessRuleComponent createEvaluateRule(
            EvaluateStatement eval,
            String context
    ) {
        String ruleId = "RULE-EVAL-" + (++ruleCounter);

        List<String> inputFields = extractFieldsFromExpression(eval.expression());
        List<String> outputFields = new ArrayList<>();

        for (EvaluateStatement.EvaluateWhenBranch branch : eval.branches()) {
            inputFields.addAll(extractFieldsFromExpression(branch.condition()));
            for (CobolStatement stmt : branch.statements()) {
                outputFields.addAll(extractOutputFields(stmt));
            }
        }

        return new DecomposedBusinessLogic.BusinessRuleComponent(
                ruleId,
                "Evaluate" + toPascalCase(context),
                "Multi-way branch on: " + eval.expression(),
                DecomposedBusinessLogic.BusinessRuleComponent.RuleType.CONDITIONAL,
                inputFields.stream().distinct().collect(Collectors.toList()),
                outputFields.stream().distinct().collect(Collectors.toList()),
                eval.expression(),
                List.of(eval)
        );
    }

    /**
     * Extracts business rules from control break patterns.
     */
    private List<DecomposedBusinessLogic.BusinessRuleComponent> extractRulesFromControlBreak(
            ControlBreakPattern pattern
    ) {
        List<DecomposedBusinessLogic.BusinessRuleComponent> rules = new ArrayList<>();

        for (ControlBreakPattern.BreakLevel level : pattern.breakLevels()) {
            for (CobolStatement stmt : level.breakStatements()) {
                DecomposedBusinessLogic.BusinessRuleComponent rule =
                        convertToBusinessRule(stmt, "BreakLevel" + level.level());
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }

        return rules;
    }

    /**
     * Extracts aggregation components from control break patterns.
     */
    private List<DecomposedBusinessLogic.AggregationComponent> extractAggregations(
            CobolIntermediateModel model
    ) {
        List<DecomposedBusinessLogic.AggregationComponent> aggregations = new ArrayList<>();

        for (ControlBreakPattern pattern : model.getControlBreakPatterns()) {
            for (ControlBreakPattern.BreakLevel level : pattern.breakLevels()) {
                if (!level.aggregations().isEmpty()) {
                    String aggId = "AGG-" + (++aggregationCounter);

                    List<DecomposedBusinessLogic.AggregationOperation> operations = level.aggregations().stream()
                            .map(this::convertToAggregationOperation)
                            .collect(Collectors.toList());

                    aggregations.add(new DecomposedBusinessLogic.AggregationComponent(
                            aggId,
                            "Level" + level.level() + "Aggregation",
                            List.of(level.controlField()),
                            operations,
                            level.level()
                    ));
                }
            }
        }

        return aggregations;
    }

    /**
     * Converts a control break aggregation to a decomposed aggregation operation.
     */
    private DecomposedBusinessLogic.AggregationOperation convertToAggregationOperation(
            ControlBreakPattern.AggregationField agg
    ) {
        DecomposedBusinessLogic.AggregationOperation.OperationType opType =
                switch (agg.aggregationType()) {
                    case SUM -> DecomposedBusinessLogic.AggregationOperation.OperationType.SUM;
                    case COUNT -> DecomposedBusinessLogic.AggregationOperation.OperationType.COUNT;
                    case AVERAGE -> DecomposedBusinessLogic.AggregationOperation.OperationType.AVERAGE;
                    case MIN -> DecomposedBusinessLogic.AggregationOperation.OperationType.MIN;
                    case MAX -> DecomposedBusinessLogic.AggregationOperation.OperationType.MAX;
                    default -> DecomposedBusinessLogic.AggregationOperation.OperationType.COLLECT;
                };

        return new DecomposedBusinessLogic.AggregationOperation(
                toCamelCase(agg.fieldName()),
                opType,
                agg.sourceField(),
                agg.fieldName()
        );
    }

    /**
     * Extracts validation components from the model.
     */
    private List<DecomposedBusinessLogic.ValidationComponent> extractValidations(
            CobolIntermediateModel model
    ) {
        List<DecomposedBusinessLogic.ValidationComponent> validations = new ArrayList<>();

        // Look for validation patterns in IF statements
        for (CobolParagraph paragraph : model.getParagraphs().values()) {
            for (CobolStatement stmt : paragraph.statements()) {
                if (stmt instanceof IfStatement ifStmt) {
                    DecomposedBusinessLogic.ValidationComponent validation =
                            extractValidationFromIf(ifStmt);
                    if (validation != null) {
                        validations.add(validation);
                    }
                }
            }
        }

        // Add implied validations from data item definitions
        for (CobolDataItem item : model.getDataItems()) {
            if (item.getPicture() != null && !item.getPicture().isEmpty()) {
                DecomposedBusinessLogic.ValidationComponent validation =
                        createPictureValidation(item);
                if (validation != null) {
                    validations.add(validation);
                }
            }
        }

        return validations;
    }

    /**
     * Extracts validation from an IF statement if it looks like a validation check.
     */
    private DecomposedBusinessLogic.ValidationComponent extractValidationFromIf(IfStatement ifStmt) {
        String condition = ifStmt.condition().toUpperCase();

        // Look for common validation patterns
        if (condition.contains("NOT NUMERIC") || condition.contains("NOT ALPHABETIC") ||
            condition.contains("SPACES") || condition.contains("ZEROS") ||
            condition.contains("LOW-VALUES") || condition.contains("HIGH-VALUES")) {

            String validationId = "VAL-" + (++validationCounter);

            // Extract field name from condition
            String fieldName = extractPrimaryField(condition);

            return new DecomposedBusinessLogic.ValidationComponent(
                    validationId,
                    fieldName,
                    ifStmt.condition(),
                    "Invalid value for " + fieldName,
                    DecomposedBusinessLogic.ValidationComponent.ValidationType.FORMAT
            );
        }

        // Look for range checks
        if (condition.contains(">") || condition.contains("<") ||
            condition.contains("GREATER") || condition.contains("LESS")) {

            String validationId = "VAL-" + (++validationCounter);
            String fieldName = extractPrimaryField(condition);

            return new DecomposedBusinessLogic.ValidationComponent(
                    validationId,
                    fieldName,
                    ifStmt.condition(),
                    "Value out of range for " + fieldName,
                    DecomposedBusinessLogic.ValidationComponent.ValidationType.RANGE
            );
        }

        return null;
    }

    /**
     * Creates a validation from a data item's PIC clause.
     */
    private DecomposedBusinessLogic.ValidationComponent createPictureValidation(CobolDataItem item) {
        String pic = item.getPicture().toUpperCase();
        String validationId = "VAL-PIC-" + (++validationCounter);

        if (pic.contains("9")) {
            return new DecomposedBusinessLogic.ValidationComponent(
                    validationId,
                    item.getName(),
                    "NUMERIC(" + pic + ")",
                    "Field must be numeric",
                    DecomposedBusinessLogic.ValidationComponent.ValidationType.FORMAT
            );
        }

        return null;
    }

    /**
     * Builds metadata about the decomposition.
     */
    private Map<String, Object> buildMetadata(CobolIntermediateModel model) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("originalProgramId", model.getProgramId());
        metadata.put("paragraphCount", model.getParagraphs().size());
        metadata.put("dataItemCount", model.getDataItems().size());
        metadata.put("controlBreakPatternCount", model.getControlBreakPatterns().size());
        metadata.put("decompositionTimestamp", System.currentTimeMillis());

        // Identify file dependencies
        Set<String> fileNames = new LinkedHashSet<>();
        for (CobolParagraph paragraph : model.getParagraphs().values()) {
            for (CobolStatement stmt : paragraph.statements()) {
                if (stmt instanceof FileOperationStatement fop) {
                    fileNames.add(fop.fileName());
                }
            }
        }
        metadata.put("fileDependencies", new ArrayList<>(fileNames));

        return metadata;
    }

    // --- Helper methods ---

    private boolean isSaveOperation(MoveStatement move) {
        String source = move.source().toUpperCase();
        String target = move.target().toUpperCase();

        return (target.startsWith("SAVE-") && target.endsWith(source)) ||
               (target.startsWith("PREV-") && target.endsWith(source)) ||
               (target.startsWith("OLD-") && target.endsWith(source)) ||
               (target.contains("-SAVE") && source.contains(target.replace("-SAVE", ""))) ||
               (target.contains("-PREV") && source.contains(target.replace("-PREV", "")));
    }

    private List<String> extractFieldsFromExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return List.of();
        }

        // Simple extraction: split on operators and keep alphanumeric tokens
        String[] tokens = expression.split("[+\\-*/()=<>\\s]+");
        return Arrays.stream(tokens)
                .filter(t -> !t.isEmpty())
                .filter(t -> !t.matches("\\d+(\\.\\d+)?")) // Exclude numeric literals
                .filter(t -> !t.startsWith("\"") && !t.startsWith("'")) // Exclude string literals
                .collect(Collectors.toList());
    }

    private List<String> extractOutputFields(CobolStatement stmt) {
        List<String> outputs = new ArrayList<>();
        if (stmt instanceof MoveStatement move) {
            outputs.add(move.target());
        } else if (stmt instanceof ComputeStatement compute) {
            outputs.add(compute.target());
        }
        return outputs;
    }

    private String extractPrimaryField(String condition) {
        String[] tokens = condition.split("[\\s=<>()]+");
        for (String token : tokens) {
            if (!token.isEmpty() && token.matches("[A-Z][A-Z0-9-]*")) {
                return token;
            }
        }
        return "UNKNOWN";
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] parts = input.toLowerCase().split("[-_]");
        if (parts.length == 0) {
            return input.toLowerCase();
        }
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String camel = toCamelCase(input);
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    private String toEntityName(String fileName) {
        return toPascalCase(fileName.replace("-FILE", "").replace("FILE-", ""));
    }
}
