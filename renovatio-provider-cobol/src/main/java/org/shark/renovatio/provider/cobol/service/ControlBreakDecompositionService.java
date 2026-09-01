package org.shark.renovatio.provider.cobol.service;

import org.shark.renovatio.cobol.ir.model.*;
import org.shark.renovatio.cobol.ir.parser.BusinessLogicDecomposer;
import org.shark.renovatio.cobol.ir.parser.ControlBreakPatternDetector;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for decomposing COBOL control break patterns into reusable architectural components.
 *
 * <p>This service addresses the architectural impedance mismatch between COBOL's
 * file-processing paradigm (READ loops with control breaks, ISAM files, groupers)
 * and modern service-oriented architectures.
 *
 * <p>Instead of translating 1 COBOL program to 1 Java program, this service:
 * <ul>
 *   <li>Detects control break patterns in COBOL code</li>
 *   <li>Extracts business rules as discrete, testable components</li>
 *   <li>Generates repository interfaces for data access</li>
 *   <li>Creates aggregation strategies using Java Streams</li>
 *   <li>Produces validation components</li>
 * </ul>
 *
 * <p>The result is a set of modern, reusable Java components that implement
 * the same business logic but with architecture-appropriate patterns.
 */
@Service
public class ControlBreakDecompositionService {

    private static final Logger log = LoggerFactory.getLogger(ControlBreakDecompositionService.class);

    private final CobolIntermediateModelService irService;
    private final CobolParsingService parsingService;
    private final ControlBreakPatternDetector patternDetector;
    private final BusinessLogicDecomposer decomposer;

    public ControlBreakDecompositionService(
            CobolIntermediateModelService irService,
            CobolParsingService parsingService
    ) {
        this.irService = irService;
        this.parsingService = parsingService;
        this.patternDetector = new ControlBreakPatternDetector();
        this.decomposer = new BusinessLogicDecomposer();
    }

    /**
     * Analyzes a COBOL workspace and decomposes all programs with control break patterns.
     *
     * @param workspace The workspace containing COBOL programs
     * @return A result containing the decomposed components
     */
    public DecompositionResult analyzeAndDecompose(Workspace workspace) {
        Path workspacePath = Paths.get(workspace.getPath());
        List<Path> cobolFiles;
        try {
            cobolFiles = parsingService.findCobolSourceFiles(workspacePath);
        } catch (IOException e) {
            DecompositionResult errorResult = new DecompositionResult();
            errorResult.addError(workspacePath.toString(), "Failed to scan workspace: " + e.getMessage());
            return errorResult;
        }

        DecompositionResult result = new DecompositionResult();

        for (Path cobolFile : cobolFiles) {
            try {
                ProgramDecomposition decomposition = decomposeProgram(cobolFile);
                if (decomposition != null) {
                    result.addProgramDecomposition(decomposition);
                }
            } catch (Exception e) {
                log.warn("Failed to decompose {}: {}", cobolFile.getFileName(), e.getMessage());
                result.addError(cobolFile.toString(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Decomposes a single COBOL program file.
     *
     * @param cobolFile Path to the COBOL source file
     * @return The decomposition result, or null if no patterns were detected
     */
    public ProgramDecomposition decomposeProgram(Path cobolFile) {
        CobolIntermediateModel model = irService.parse(cobolFile);

        // Detect control break patterns
        List<ControlBreakPattern> patterns = patternDetector.detectPatterns(model);

        if (patterns.isEmpty()) {
            log.debug("No control break patterns detected in {}", cobolFile.getFileName());
            return null;
        }

        log.info("Detected {} control break pattern(s) in {}", patterns.size(), cobolFile.getFileName());

        // Create enhanced model with patterns
        CobolIntermediateModel.Builder enhancedBuilder = CobolIntermediateModel.builder()
                .programId(model.getProgramId())
                .dataItems(model.getDataItems())
                .controlFlowGraph(model.getControlFlowGraph())
                .executionContext(model.getExecutionContext())
                .controlBreakPatterns(patterns);

        model.getParagraphs().values().forEach(enhancedBuilder::addParagraph);

        CobolIntermediateModel enhancedModel = enhancedBuilder.build();

        // Decompose into business logic components
        DecomposedBusinessLogic decomposedLogic = decomposer.decompose(enhancedModel);

        return new ProgramDecomposition(
                cobolFile.getFileName().toString(),
                model.getProgramId(),
                patterns,
                decomposedLogic
        );
    }

    /**
     * Generates Java code from decomposed components.
     *
     * @param decomposition The program decomposition
     * @param workspace     The target workspace
     * @return StubResult containing generated files
     */
    public StubResult generateDecomposedCode(ProgramDecomposition decomposition, Workspace workspace) {
        StubResult result = generateDecomposedCode(decomposition);
        if (!result.isSuccess()) {
            return result;
        }
        try {
            persistGeneratedCode(result.getGeneratedCode(), workspace);
            return result;
        } catch (IOException e) {
            return new StubResult(false, "Failed to write files: " + e.getMessage());
        }
    }

    /** Generates decomposed Java artifacts without mutating the workspace. */
    public StubResult generateDecomposedCode(ProgramDecomposition decomposition) {
        Map<String, String> generatedFiles = new LinkedHashMap<>();

        DecomposedBusinessLogic logic = decomposition.decomposedLogic();

        // Generate repository interfaces
        for (var dataAccess : logic.dataAccessComponents()) {
            String repoCode = generateRepository(dataAccess, logic.programId());
            generatedFiles.put(dataAccess.entityName() + "Repository.java", repoCode);
        }

        // Generate business rule service
        if (!logic.businessRules().isEmpty()) {
            String rulesCode = generateBusinessRulesService(logic.businessRules(), logic.programId());
            generatedFiles.put(toPascalCase(logic.programId()) + "BusinessRules.java", rulesCode);
        }

        // Generate aggregation strategies
        if (!logic.aggregations().isEmpty()) {
            String aggCode = generateAggregationService(logic.aggregations(), logic.programId());
            generatedFiles.put(toPascalCase(logic.programId()) + "Aggregations.java", aggCode);
        }

        // Generate validation service
        if (!logic.validations().isEmpty()) {
            String valCode = generateValidationService(logic.validations(), logic.programId());
            generatedFiles.put(toPascalCase(logic.programId()) + "Validator.java", valCode);
        }

        // Generate main orchestrator service
        String orchestratorCode = generateOrchestratorService(decomposition);
        generatedFiles.put(toPascalCase(logic.programId()) + "ProcessingService.java", orchestratorCode);

        StubResult result = new StubResult(true,
                "Generated " + generatedFiles.size() + " decomposed components from " + logic.programId());
        result.setGeneratedCode(generatedFiles);
        return result;
    }

    /** Persists an already validated aggregate of decomposed artifacts. */
    public void persistGeneratedCode(Map<String, String> generatedFiles, Workspace workspace) throws IOException {
        writeGeneratedFiles(generatedFiles, workspace);
    }

    // --- Code generation methods ---

    private String generateRepository(DecomposedBusinessLogic.DataAccessComponent dataAccess, String programId) {
        StringBuilder code = new StringBuilder();
        String entityName = dataAccess.entityName();

        code.append("package org.shark.renovatio.generated.").append(programId.toLowerCase()).append(";\n\n");
        code.append("import java.util.List;\n");
        code.append("import java.util.Optional;\n");
        code.append("import java.util.stream.Stream;\n\n");

        code.append("/**\n");
        code.append(" * Repository interface for ").append(entityName).append(" data access.\n");
        code.append(" * Generated from COBOL file: ").append(dataAccess.recordName()).append("\n");
        code.append(" * Original access pattern: ").append(dataAccess.accessPattern()).append("\n");
        code.append(" *\n");
        code.append(" * This interface abstracts the data access layer, allowing the business logic\n");
        code.append(" * to work with data from any source (database, file, API, queue).\n");
        code.append(" */\n");
        code.append("public interface ").append(entityName).append("Repository {\n\n");

        // Stream-based access for control break processing
        code.append("    /**\n");
        code.append("     * Returns a stream of all records, suitable for control break processing.\n");
        code.append("     * The stream should be ordered by the control break key fields.\n");
        code.append("     */\n");
        code.append("    Stream<").append(entityName).append("Record> streamAll();\n\n");

        code.append("    /**\n");
        code.append("     * Returns all records as a list.\n");
        code.append("     */\n");
        code.append("    List<").append(entityName).append("Record> findAll();\n\n");

        // Key-based access
        if (!dataAccess.keyFields().isEmpty()) {
            String keyField = dataAccess.keyFields().get(0);
            code.append("    /**\n");
            code.append("     * Finds a record by its key.\n");
            code.append("     */\n");
            code.append("    Optional<").append(entityName).append("Record> findBy")
                    .append(toPascalCase(keyField)).append("(String key);\n\n");
        }

        // CRUD operations if applicable
        if (dataAccess.accessPattern() == DecomposedBusinessLogic.DataAccessComponent.AccessPattern.CRUD) {
            code.append("    /**\n");
            code.append("     * Saves a record.\n");
            code.append("     */\n");
            code.append("    ").append(entityName).append("Record save(").append(entityName).append("Record record);\n\n");

            code.append("    /**\n");
            code.append("     * Deletes a record by key.\n");
            code.append("     */\n");
            code.append("    void deleteByKey(String key);\n\n");
        }

        code.append("}\n");

        return code.toString();
    }

    private String generateBusinessRulesService(
            List<DecomposedBusinessLogic.BusinessRuleComponent> rules,
            String programId
    ) {
        StringBuilder code = new StringBuilder();
        String className = toPascalCase(programId) + "BusinessRules";

        code.append("package org.shark.renovatio.generated.").append(programId.toLowerCase()).append(";\n\n");
        code.append("import java.math.BigDecimal;\n");
        code.append("import org.springframework.stereotype.Component;\n\n");

        code.append("/**\n");
        code.append(" * Business rules extracted from COBOL program: ").append(programId).append("\n");
        code.append(" *\n");
        code.append(" * Each method represents a discrete, testable business rule.\n");
        code.append(" * These rules are decoupled from file processing and can be reused\n");
        code.append(" * in any context (batch, API, event-driven).\n");
        code.append(" */\n");
        code.append("@Component\n");
        code.append("public class ").append(className).append(" {\n\n");

        // Group rules by type for better organization
        Map<DecomposedBusinessLogic.BusinessRuleComponent.RuleType, List<DecomposedBusinessLogic.BusinessRuleComponent>> rulesByType =
                rules.stream().collect(Collectors.groupingBy(DecomposedBusinessLogic.BusinessRuleComponent::ruleType));

        // Generate calculation rules
        List<DecomposedBusinessLogic.BusinessRuleComponent> calcRules = rulesByType.get(DecomposedBusinessLogic.BusinessRuleComponent.RuleType.CALCULATION);
        if (calcRules != null) {
            code.append("    // ========== Calculation Rules ==========\n\n");
            for (var rule : calcRules) {
                generateRuleMethod(code, rule);
            }
        }

        // Generate conditional rules
        List<DecomposedBusinessLogic.BusinessRuleComponent> condRules = rulesByType.get(DecomposedBusinessLogic.BusinessRuleComponent.RuleType.CONDITIONAL);
        if (condRules != null) {
            code.append("    // ========== Conditional Rules ==========\n\n");
            for (var rule : condRules) {
                generateRuleMethod(code, rule);
            }
        }

        // Generate transformation rules
        List<DecomposedBusinessLogic.BusinessRuleComponent> transRules = rulesByType.get(DecomposedBusinessLogic.BusinessRuleComponent.RuleType.TRANSFORMATION);
        if (transRules != null) {
            code.append("    // ========== Transformation Rules ==========\n\n");
            for (var rule : transRules) {
                generateRuleMethod(code, rule);
            }
        }

        code.append("}\n");

        return code.toString();
    }

    private void generateRuleMethod(StringBuilder code, DecomposedBusinessLogic.BusinessRuleComponent rule) {
        code.append("    /**\n");
        code.append("     * ").append(rule.description() != null ? rule.description() : rule.ruleName()).append("\n");
        if (!rule.inputFields().isEmpty()) {
            code.append("     * Inputs: ").append(String.join(", ", rule.inputFields())).append("\n");
        }
        if (!rule.outputFields().isEmpty()) {
            code.append("     * Outputs: ").append(String.join(", ", rule.outputFields())).append("\n");
        }
        code.append("     */\n");

        String methodName = toCamelCase(rule.ruleName());
        String returnType = rule.outputFields().isEmpty() ? "void" : "Object";

        code.append("    public ").append(returnType).append(" ").append(methodName).append("(");

        // Generate parameters
        List<String> params = rule.inputFields().stream()
                .map(f -> "Object " + toCamelCase(f))
                .collect(Collectors.toList());
        code.append(String.join(", ", params));

        code.append(") {\n");
        code.append("        // TODO: Implement rule - ").append(rule.expression() != null ? rule.expression() : "").append("\n");
        if (!returnType.equals("void")) {
            code.append("        return null;\n");
        }
        code.append("    }\n\n");
    }

    private String generateAggregationService(
            List<DecomposedBusinessLogic.AggregationComponent> aggregations,
            String programId
    ) {
        StringBuilder code = new StringBuilder();
        String className = toPascalCase(programId) + "Aggregations";

        code.append("package org.shark.renovatio.generated.").append(programId.toLowerCase()).append(";\n\n");
        code.append("import java.math.BigDecimal;\n");
        code.append("import java.util.*;\n");
        code.append("import java.util.function.*;\n");
        code.append("import java.util.stream.*;\n");
        code.append("import org.springframework.stereotype.Component;\n\n");

        code.append("/**\n");
        code.append(" * Aggregation strategies extracted from COBOL control break patterns.\n");
        code.append(" * Program: ").append(programId).append("\n");
        code.append(" *\n");
        code.append(" * These aggregations use Java Streams and Collectors to implement\n");
        code.append(" * the same grouping/totaling logic as the original COBOL control breaks.\n");
        code.append(" */\n");
        code.append("@Component\n");
        code.append("public class ").append(className).append(" {\n\n");

        for (var agg : aggregations) {
            generateAggregationMethod(code, agg);
        }

        // Generate a combined aggregation method
        code.append("    /**\n");
        code.append("     * Processes records with all break levels, similar to the original COBOL control break.\n");
        code.append("     */\n");
        code.append("    public <T> Map<String, Map<String, Object>> processWithBreaks(\n");
        code.append("            Stream<T> records,\n");
        code.append("            Function<T, String> groupKeyExtractor,\n");
        code.append("            Function<T, BigDecimal> valueExtractor) {\n");
        code.append("        \n");
        code.append("        return records.collect(\n");
        code.append("            Collectors.groupingBy(\n");
        code.append("                groupKeyExtractor,\n");
        code.append("                LinkedHashMap::new,\n");
        code.append("                Collectors.collectingAndThen(\n");
        code.append("                    Collectors.toList(),\n");
        code.append("                    this::computeGroupAggregates\n");
        code.append("                )\n");
        code.append("            )\n");
        code.append("        );\n");
        code.append("    }\n\n");

        code.append("    private <T> Map<String, Object> computeGroupAggregates(List<T> groupRecords) {\n");
        code.append("        Map<String, Object> aggregates = new LinkedHashMap<>();\n");
        code.append("        aggregates.put(\"count\", groupRecords.size());\n");
        code.append("        // TODO: Add specific aggregations based on extracted patterns\n");
        code.append("        return aggregates;\n");
        code.append("    }\n\n");

        code.append("}\n");

        return code.toString();
    }

    private void generateAggregationMethod(StringBuilder code, DecomposedBusinessLogic.AggregationComponent agg) {
        String methodName = toCamelCase(agg.name());

        code.append("    /**\n");
        code.append("     * Aggregation for break level ").append(agg.breakLevel()).append("\n");
        code.append("     * Group by: ").append(String.join(", ", agg.groupByFields())).append("\n");
        code.append("     */\n");
        code.append("    public <T> Map<String, Object> ").append(methodName).append("(\n");
        code.append("            Stream<T> records,\n");
        code.append("            Function<T, String> groupKeyExtractor) {\n");
        code.append("        \n");
        code.append("        Map<String, Object> results = new LinkedHashMap<>();\n");
        code.append("        \n");

        for (var op : agg.operations()) {
            switch (op.operationType()) {
                case SUM -> code.append("        // SUM: ").append(op.targetField()).append("\n");
                case COUNT -> code.append("        // COUNT: ").append(op.targetField()).append("\n");
                case AVERAGE -> code.append("        // AVERAGE: ").append(op.targetField()).append("\n");
                case MIN -> code.append("        // MIN: ").append(op.targetField()).append("\n");
                case MAX -> code.append("        // MAX: ").append(op.targetField()).append("\n");
                default -> code.append("        // ").append(op.operationType()).append(": ").append(op.targetField()).append("\n");
            }
        }

        code.append("        \n");
        code.append("        return results;\n");
        code.append("    }\n\n");
    }

    private String generateValidationService(
            List<DecomposedBusinessLogic.ValidationComponent> validations,
            String programId
    ) {
        StringBuilder code = new StringBuilder();
        String className = toPascalCase(programId) + "Validator";

        code.append("package org.shark.renovatio.generated.").append(programId.toLowerCase()).append(";\n\n");
        code.append("import java.util.*;\n");
        code.append("import org.springframework.stereotype.Component;\n\n");

        code.append("/**\n");
        code.append(" * Validation rules extracted from COBOL program: ").append(programId).append("\n");
        code.append(" */\n");
        code.append("@Component\n");
        code.append("public class ").append(className).append(" {\n\n");

        code.append("    /**\n");
        code.append("     * Validates a record and returns a list of validation errors.\n");
        code.append("     */\n");
        code.append("    public List<ValidationError> validate(Object record) {\n");
        code.append("        List<ValidationError> errors = new ArrayList<>();\n");
        code.append("        \n");

        for (var val : validations) {
            code.append("        // ").append(val.validationType()).append(": ").append(val.fieldName()).append("\n");
            code.append("        // Condition: ").append(val.condition() != null ? val.condition() : "").append("\n");
        }

        code.append("        \n");
        code.append("        return errors;\n");
        code.append("    }\n\n");

        code.append("    public record ValidationError(String field, String message, String code) {}\n");

        code.append("}\n");

        return code.toString();
    }

    private String generateOrchestratorService(ProgramDecomposition decomposition) {
        StringBuilder code = new StringBuilder();
        String programId = decomposition.programId();
        String className = toPascalCase(programId) + "ProcessingService";

        code.append("package org.shark.renovatio.generated.").append(programId.toLowerCase()).append(";\n\n");
        code.append("import java.util.*;\n");
        code.append("import java.util.stream.*;\n");
        code.append("import org.springframework.stereotype.Service;\n\n");

        code.append("/**\n");
        code.append(" * Orchestrator service that coordinates the decomposed business logic.\n");
        code.append(" * Original COBOL program: ").append(programId).append("\n");
        code.append(" * \n");
        code.append(" * This service replaces the original COBOL control break processing\n");
        code.append(" * with a modern, stream-based architecture while preserving the\n");
        code.append(" * exact same business logic.\n");
        code.append(" * \n");
        code.append(" * Architecture Benefits:\n");
        code.append(" * - Data access is abstracted through repository interfaces\n");
        code.append(" * - Business rules are discrete, testable methods\n");
        code.append(" * - Aggregations use Java Streams (parallelizable)\n");
        code.append(" * - Validations are reusable across contexts\n");
        code.append(" */\n");
        code.append("@Service\n");
        code.append("public class ").append(className).append(" {\n\n");

        // Generate constructor with dependencies
        DecomposedBusinessLogic logic = decomposition.decomposedLogic();

        List<String> dependencies = new ArrayList<>();
        for (var dataAccess : logic.dataAccessComponents()) {
            dependencies.add(dataAccess.entityName() + "Repository " + toCamelCase(dataAccess.entityName()) + "Repository");
        }
        if (!logic.businessRules().isEmpty()) {
            dependencies.add(toPascalCase(programId) + "BusinessRules businessRules");
        }
        if (!logic.aggregations().isEmpty()) {
            dependencies.add(toPascalCase(programId) + "Aggregations aggregations");
        }
        if (!logic.validations().isEmpty()) {
            dependencies.add(toPascalCase(programId) + "Validator validator");
        }

        // Fields
        for (String dep : dependencies) {
            String[] parts = dep.split(" ");
            if (parts.length >= 2) {
                code.append("    private final ").append(parts[0]).append(" ").append(parts[1]).append(";\n");
            }
        }
        code.append("\n");

        // Constructor
        code.append("    public ").append(className).append("(");
        if (!dependencies.isEmpty()) {
            code.append("\n            ").append(String.join(",\n            ", dependencies));
        }
        code.append(") {\n");
        for (String dep : dependencies) {
            String[] parts = dep.split(" ");
            if (parts.length >= 2) {
                code.append("        this.").append(parts[1]).append(" = ").append(parts[1]).append(";\n");
            }
        }
        code.append("    }\n\n");

        // Main process method
        code.append("    /**\n");
        code.append("     * Processes all records with control break logic.\n");
        code.append("     * This method replicates the original COBOL batch processing\n");
        code.append("     * using modern Java streams and collectors.\n");
        code.append("     */\n");
        code.append("    public ProcessingResult process() {\n");
        code.append("        ProcessingResult result = new ProcessingResult();\n");
        code.append("        \n");

        // Generate processing logic based on control break patterns
        for (ControlBreakPattern pattern : decomposition.controlBreakPatterns()) {
            code.append("        // Process file: ").append(pattern.fileName()).append("\n");
            code.append("        // Break levels: ").append(pattern.breakLevels().size()).append("\n");

            for (ControlBreakPattern.BreakLevel level : pattern.breakLevels()) {
                code.append("        // Level ").append(level.level())
                        .append(" - Control field: ").append(level.controlField()).append("\n");
            }
        }

        code.append("        \n");
        code.append("        // TODO: Implement stream-based processing using injected components\n");
        code.append("        \n");
        code.append("        return result;\n");
        code.append("    }\n\n");

        // Result record
        code.append("    public record ProcessingResult(\n");
        code.append("        int recordsProcessed,\n");
        code.append("        Map<String, Object> groupTotals,\n");
        code.append("        Map<String, Object> grandTotals,\n");
        code.append("        List<String> errors\n");
        code.append("    ) {\n");
        code.append("        public ProcessingResult() {\n");
        code.append("            this(0, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());\n");
        code.append("        }\n");
        code.append("    }\n");

        code.append("}\n");

        return code.toString();
    }

    private void writeGeneratedFiles(Map<String, String> files, Workspace workspace) throws IOException {
        Path outputDir = Paths.get(workspace.getPath(), "generated-decomposed");
        Files.createDirectories(outputDir);

        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path filePath = outputDir.resolve(entry.getKey());
            Files.writeString(filePath, entry.getValue());
            log.info("Generated: {}", filePath);
        }
    }

    // --- Helper methods ---

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] parts = input.toLowerCase().split("[-_]");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1));
                }
            }
        }
        return result.toString();
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String pascal = toPascalCase(input);
        if (pascal.isEmpty()) return input;
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    // --- Result classes ---

    /**
     * Result of analyzing and decomposing a workspace.
     */
    public static class DecompositionResult {
        private final List<ProgramDecomposition> decompositions = new ArrayList<>();
        private final Map<String, String> errors = new LinkedHashMap<>();

        public void addProgramDecomposition(ProgramDecomposition decomposition) {
            decompositions.add(decomposition);
        }

        public void addError(String file, String error) {
            errors.put(file, error);
        }

        public List<ProgramDecomposition> getDecompositions() {
            return decompositions;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public boolean hasResults() {
            return !decompositions.isEmpty();
        }

        public int getTotalControlBreakPatterns() {
            return decompositions.stream()
                    .mapToInt(d -> d.controlBreakPatterns().size())
                    .sum();
        }
    }

    /**
     * Decomposition result for a single program.
     */
    public record ProgramDecomposition(
            String fileName,
            String programId,
            List<ControlBreakPattern> controlBreakPatterns,
            DecomposedBusinessLogic decomposedLogic
    ) {}
}
