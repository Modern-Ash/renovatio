package org.shark.renovatio.cobol.ir.annotated;

import org.shark.renovatio.cobol.ir.model.*;
import org.shark.renovatio.cobol.ir.context.CobolExecutionContext;
import org.shark.renovatio.cobol.ir.flow.ControlFlowGraph;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds the closed canonical identity envelopes for a schema-valid {@code cobol-ir.v1} projection. */
public final class CobolIrIdentityProjector {

    public static final String BASE_IR_VERSION = "cobol-ir.v1";

    public record ProjectedNode(String nodeId, AnnotatedNodeKind nodeKind, String pointer,
                                Map<String, ?> semanticContent) { }

    /** Projects the authoritative, annotation-relevant base IR without caller-supplied identity data. */
    public Map<String, Object> project(CobolIntermediateModel model) {
        Objects.requireNonNull(model, "model");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", BASE_IR_VERSION);
        result.put("programId", model.getProgramId());
        Map<String, Object> paragraphs = new LinkedHashMap<>();
        model.getParagraphs().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> paragraphs.put(entry.getKey(), paragraph(entry.getValue())));
        result.put("paragraphs", paragraphs);
        result.put("dataItems", model.getDataItems().stream().map(this::dataItem).toList());
        result.put("controlFlowGraph", controlFlowGraph(model.getControlFlowGraph()));
        result.put("executionContext", executionContext(model.getExecutionContext()));
        result.put("controlBreakPatterns", model.getControlBreakPatterns().stream()
                .map(this::controlBreakPattern).toList());
        result.put("decomposedLogic", model.getDecomposedLogic() == null
                ? null : decomposedLogic(model.getDecomposedLogic()));
        result.put("diagnostics", model.getDiagnostics().stream().map(this::diagnostic).toList());
        return result;
    }

    public String baseIrHash(CobolIntermediateModel model) {
        return AnnotatedIdentity.hashCanonical(project(model));
    }

    /** Projects any closed-set identity-bearing type and fails closed for unknown Java types. */
    public ProjectedNode node(Object value, String pointer) {
        Objects.requireNonNull(value, "value");
        AnnotatedNodeKind kind = nodeKind(value);
        Map<String, Object> content = semanticContent(value);
        SourceSpan span = sourceSpanOf(value);
        return new ProjectedNode(nodeId(kind, pointer, span, content), kind, pointer, content);
    }

    /** Enumerates every identity-bearing node reachable from the current base model. */
    public List<ProjectedNode> nodes(CobolIntermediateModel model) {
        Objects.requireNonNull(model, "model");
        List<ProjectedNode> nodes = new ArrayList<>();
        for (int i = 0; i < model.getDataItems().size(); i++) {
            CobolDataItem item = model.getDataItems().get(i);
            String itemPath = "/dataItems/" + i;
            add(nodes, AnnotatedNodeKind.DATA_ITEM, itemPath, dataItem(item));
            for (int j = 0; j < item.level88Conditions().size(); j++) {
                Level88Condition condition = item.level88Conditions().get(j);
                String conditionPath = itemPath + "/level88Conditions/" + j;
                add(nodes, AnnotatedNodeKind.LEVEL_88_CONDITION, conditionPath, level88Condition(condition));
                for (int k = 0; k < condition.values().size(); k++) {
                    add(nodes, AnnotatedNodeKind.LEVEL_88_VALUE, conditionPath + "/values/" + k,
                            level88Value(condition.values().get(k)));
                }
            }
        }
        model.getParagraphs().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String path = childPointer("/paragraphs", entry.getKey());
            add(nodes, AnnotatedNodeKind.PARAGRAPH, path, paragraph(entry.getValue()));
            addStatements(nodes, path + "/statements", entry.getValue().statements());
        });
        return List.copyOf(nodes);
    }

    private void addStatements(List<ProjectedNode> nodes, String parent, List<CobolStatement> statements) {
        for (int i = 0; i < statements.size(); i++) {
            CobolStatement statement = statements.get(i);
            String path = parent + "/" + i;
            AnnotatedNodeKind kind = statementKind(statement);
            add(nodes, kind, path, statement(statement));
            if (statement instanceof IfStatement value) {
                addStatements(nodes, path + "/thenStatements", value.thenStatements());
                addStatements(nodes, path + "/elseStatements", value.elseStatements());
            } else if (statement instanceof EvaluateStatement value) {
                for (int j = 0; j < value.branches().size(); j++) {
                    EvaluateStatement.EvaluateWhenBranch branch = value.branches().get(j);
                    String branchPath = path + "/branches/" + j;
                    add(nodes, AnnotatedNodeKind.EVALUATE_BRANCH, branchPath, evaluateBranch(branch));
                    addStatements(nodes, branchPath + "/statements", branch.statements());
                }
            }
        }
    }

    private void add(List<ProjectedNode> nodes, AnnotatedNodeKind kind, String path, Map<String, ?> content) {
        nodes.add(new ProjectedNode(nodeId(kind, path, null, content), kind, path, content));
    }

    private Map<String, Object> paragraph(CobolParagraph value) {
        return map("name", value.name(), "statements", value.statements().stream().map(this::statement).toList());
    }

    private Map<String, Object> dataItem(CobolDataItem value) {
        Object pic = value.picType() == null ? null : map("category", value.picType().category().name(),
                "digits", value.picType().digits(), "scale", value.picType().scale(),
                "signed", value.picType().signed(), "usage", value.picType().usage().name());
        return map("name", value.name(), "picture", value.picture(), "level", value.level(),
                "occurs", value.occurs(), "redefines", value.redefines(), "javaType", value.javaType(),
                "picType", pic, "level88Conditions", value.level88Conditions().stream().map(this::level88Condition).toList());
    }

    private Map<String, Object> level88Condition(Level88Condition value) {
        return map("name", value.name(), "parentDataName", value.parentDataName(),
                "values", value.values().stream().map(this::level88Value).toList());
    }

    private Map<String, Object> level88Value(Level88Value value) {
        return map("value", value.value(), "through", value.through());
    }

    private Map<String, Object> diagnostic(CobolDiagnostic value) {
        return map("code", value.code(), "severity", value.severity().name(),
                "constructionFamily", value.constructionFamily(), "message", value.message(),
                "sourceSpan", sourceSpan(value.sourceSpan()));
    }

    private Map<String, Object> controlFlowGraph(ControlFlowGraph value) {
        Map<String, Object> adjacency = new LinkedHashMap<>();
        value.adjacency().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                adjacency.put(entry.getKey(), entry.getValue().stream().sorted().toList()));
        return map("adjacency", adjacency);
    }

    private Map<String, Object> executionContext(CobolExecutionContext value) {
        return map("variableScopes", sortedJsonMap(value.getVariableScopes()),
                "attributes", sortedJsonMap(value.getAttributes()));
    }

    private Map<String, Object> controlBreakPattern(ControlBreakPattern value) {
        return map("patternId", value.patternId(), "fileName", value.fileName(),
                "breakLevels", value.breakLevels().stream().map(this::breakLevel).toList(),
                "initializationStatements", value.initializationStatements().stream().map(this::statement).toList(),
                "detailProcessingStatements", value.detailProcessingStatements().stream().map(this::statement).toList(),
                "finalizationStatements", value.finalizationStatements().stream().map(this::statement).toList(),
                "fileOperationType", value.fileOperationType().name());
    }

    private Map<String, Object> breakLevel(ControlBreakPattern.BreakLevel value) {
        return map("level", value.level(), "controlField", value.controlField(),
                "previousValueField", value.previousValueField(),
                "breakStatements", value.breakStatements().stream().map(this::statement).toList(),
                "aggregations", value.aggregations().stream().map(this::aggregationField).toList());
    }

    private Map<String, Object> aggregationField(ControlBreakPattern.AggregationField value) {
        return map("fieldName", value.fieldName(), "aggregationType", value.aggregationType().name(),
                "sourceField", value.sourceField());
    }

    private Map<String, Object> decomposedLogic(DecomposedBusinessLogic value) {
        return map("programId", value.programId(),
                "dataAccessComponents", value.dataAccessComponents().stream().map(this::dataAccess).toList(),
                "businessRules", value.businessRules().stream().map(this::businessRule).toList(),
                "aggregations", value.aggregations().stream().map(this::aggregation).toList(),
                "validations", value.validations().stream().map(this::validation).toList(),
                "metadata", sortedJsonMap(value.metadata()));
    }

    private Map<String, Object> dataAccess(DecomposedBusinessLogic.DataAccessComponent value) {
        return map("componentId", value.componentId(), "entityName", value.entityName(),
                "recordName", value.recordName(), "keyFields", value.keyFields(),
                "fieldMappings", value.fieldMappings().stream().map(this::fieldMapping).toList(),
                "accessPattern", value.accessPattern().name());
    }

    private Map<String, Object> fieldMapping(DecomposedBusinessLogic.FieldMapping value) {
        return map("cobolFieldName", value.cobolFieldName(), "targetFieldName", value.targetFieldName(),
                "cobolType", value.cobolType(), "targetType", value.targetType(),
                "transformationExpression", value.transformationExpression());
    }

    private Map<String, Object> businessRule(DecomposedBusinessLogic.BusinessRuleComponent value) {
        return map("ruleId", value.ruleId(), "ruleName", value.ruleName(), "description", value.description(),
                "ruleType", value.ruleType().name(), "inputFields", value.inputFields(),
                "outputFields", value.outputFields(), "expression", value.expression(),
                "originalStatements", value.originalStatements().stream().map(this::statement).toList());
    }

    private Map<String, Object> aggregation(DecomposedBusinessLogic.AggregationComponent value) {
        return map("aggregationId", value.aggregationId(), "name", value.name(),
                "groupByFields", value.groupByFields(),
                "operations", value.operations().stream().map(this::aggregationOperation).toList(),
                "breakLevel", value.breakLevel());
    }

    private Map<String, Object> aggregationOperation(DecomposedBusinessLogic.AggregationOperation value) {
        return map("operationName", value.operationName(), "operationType", value.operationType().name(),
                "sourceField", value.sourceField(), "targetField", value.targetField());
    }

    private Map<String, Object> validation(DecomposedBusinessLogic.ValidationComponent value) {
        return map("validationId", value.validationId(), "fieldName", value.fieldName(),
                "condition", value.condition(), "errorMessage", value.errorMessage(),
                "validationType", value.validationType().name());
    }

    private static Map<String, Object> sortedJsonMap(Map<String, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), jsonValue(entry.getValue())));
        return result;
    }

    private static Object jsonValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) return value;
        if (value instanceof Enum<?> enumeration) return enumeration.name();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                if (!(key instanceof String text)) throw new IllegalArgumentException("base IR JSON object keys must be strings");
                typed.put(text, jsonValue(item));
            });
            return sortedJsonMap(typed);
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> items = new ArrayList<>();
            iterable.forEach(item -> items.add(jsonValue(item)));
            return Collections.unmodifiableList(items);
        }
        throw new IllegalArgumentException("unsupported base IR JSON value: " + value.getClass().getName());
    }

    private Map<String, Object> statement(CobolStatement value) {
        if (value instanceof MoveStatement v) return map("source", v.source(), "target", v.target());
        if (value instanceof ComputeStatement v) return map("target", v.target(), "expression", v.expression());
        if (value instanceof IfStatement v) return map("condition", v.condition(),
                "thenStatements", v.thenStatements().stream().map(this::statement).toList(),
                "elseStatements", v.elseStatements().stream().map(this::statement).toList());
        if (value instanceof EvaluateStatement v) return map("expression", v.expression(),
                "branches", v.branches().stream().map(this::evaluateBranch).toList());
        if (value instanceof PerformStatement v) return map("paragraph", v.paragraph(), "throughParagraph", v.throughParagraph());
        if (value instanceof CallStatement v) return map("target", v.target(), "arguments", v.arguments());
        if (value instanceof Db2Statement v) return map("sql", v.sql());
        if (value instanceof FileOperationStatement v) return map("operationType", v.operationType().name(), "fileName", v.fileName());
        throw new IllegalArgumentException("unmapped identity-bearing statement type: " + value.getClass().getName());
    }

    private Map<String, Object> evaluateBranch(EvaluateStatement.EvaluateWhenBranch value) {
        return map("condition", value.condition(), "statements", value.statements().stream().map(this::statement).toList());
    }

    private AnnotatedNodeKind statementKind(CobolStatement value) {
        if (value instanceof MoveStatement) return AnnotatedNodeKind.MOVE_STATEMENT;
        if (value instanceof ComputeStatement) return AnnotatedNodeKind.COMPUTE_STATEMENT;
        if (value instanceof IfStatement) return AnnotatedNodeKind.IF_STATEMENT;
        if (value instanceof EvaluateStatement) return AnnotatedNodeKind.EVALUATE_STATEMENT;
        if (value instanceof PerformStatement) return AnnotatedNodeKind.PERFORM_STATEMENT;
        if (value instanceof CallStatement) return AnnotatedNodeKind.CALL_STATEMENT;
        if (value instanceof Db2Statement) return AnnotatedNodeKind.DB2_STATEMENT;
        if (value instanceof FileOperationStatement) return AnnotatedNodeKind.FILE_OPERATION_STATEMENT;
        throw new IllegalArgumentException("unmapped identity-bearing statement type: " + value.getClass().getName());
    }

    private AnnotatedNodeKind nodeKind(Object value) {
        if (value instanceof CobolDataItem) return AnnotatedNodeKind.DATA_ITEM;
        if (value instanceof Level88Condition) return AnnotatedNodeKind.LEVEL_88_CONDITION;
        if (value instanceof Level88Value) return AnnotatedNodeKind.LEVEL_88_VALUE;
        if (value instanceof CobolParagraph) return AnnotatedNodeKind.PARAGRAPH;
        if (value instanceof CobolStatement statement) return statementKind(statement);
        if (value instanceof EvaluateStatement.EvaluateWhenBranch) return AnnotatedNodeKind.EVALUATE_BRANCH;
        if (value instanceof LiteralExpression) return AnnotatedNodeKind.LITERAL_EXPRESSION;
        if (value instanceof DataReferenceExpression) return AnnotatedNodeKind.DATA_REFERENCE_EXPRESSION;
        if (value instanceof UnaryArithmeticExpression) return AnnotatedNodeKind.UNARY_ARITHMETIC_EXPRESSION;
        if (value instanceof BinaryArithmeticExpression) return AnnotatedNodeKind.BINARY_ARITHMETIC_EXPRESSION;
        if (value instanceof ComparisonCondition) return AnnotatedNodeKind.COMPARISON_CONDITION;
        if (value instanceof BooleanCondition) return AnnotatedNodeKind.BOOLEAN_CONDITION;
        if (value instanceof NegatedCondition) return AnnotatedNodeKind.NEGATED_CONDITION;
        if (value instanceof Level88ConditionReference) return AnnotatedNodeKind.LEVEL_88_CONDITION_REFERENCE;
        throw new IllegalArgumentException("unmapped identity-bearing type: " + value.getClass().getName());
    }

    private Map<String, Object> semanticContent(Object value) {
        if (value instanceof CobolDataItem v) return dataItem(v);
        if (value instanceof Level88Condition v) return level88Condition(v);
        if (value instanceof Level88Value v) return level88Value(v);
        if (value instanceof CobolParagraph v) return paragraph(v);
        if (value instanceof CobolStatement v) return statement(v);
        if (value instanceof EvaluateStatement.EvaluateWhenBranch v) return evaluateBranch(v);
        if (value instanceof LiteralExpression v) return map("kind", v.kind().name(), "value", v.value());
        if (value instanceof DataReferenceExpression v) return map("dataName", v.dataName());
        if (value instanceof UnaryArithmeticExpression v) return map("operator", v.operator().name(),
                "operand", semanticContent(v.operand()));
        if (value instanceof BinaryArithmeticExpression v) return map("left", semanticContent(v.left()),
                "operator", v.operator().name(), "right", semanticContent(v.right()));
        if (value instanceof ComparisonCondition v) return map("left", semanticContent(v.left()),
                "operator", v.operator().name(), "right", semanticContent(v.right()));
        if (value instanceof BooleanCondition v) return map("left", semanticContent(v.left()),
                "operator", v.operator().name(), "right", semanticContent(v.right()));
        if (value instanceof NegatedCondition v) return map("condition", semanticContent(v.condition()));
        if (value instanceof Level88ConditionReference v) return map("conditionName", v.conditionName());
        throw new IllegalArgumentException("unmapped identity-bearing type: " + value.getClass().getName());
    }

    private SourceSpan sourceSpanOf(Object value) {
        if (value instanceof LiteralExpression v) return v.sourceSpan();
        if (value instanceof DataReferenceExpression v) return v.sourceSpan();
        if (value instanceof UnaryArithmeticExpression v) return v.sourceSpan();
        if (value instanceof BinaryArithmeticExpression v) return v.sourceSpan();
        if (value instanceof ComparisonCondition v) return v.sourceSpan();
        if (value instanceof BooleanCondition v) return v.sourceSpan();
        if (value instanceof NegatedCondition v) return v.sourceSpan();
        if (value instanceof Level88ConditionReference v) return v.sourceSpan();
        return null;
    }

    private static Map<String, Object> map(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) result.put((String) entries[i], entries[i + 1]);
        return result;
    }

    public String nodeId(AnnotatedNodeKind nodeKind, String pointer, SourceSpan sourceSpan,
                         Map<String, ?> semanticContent) {
        Objects.requireNonNull(nodeKind, "nodeKind");
        requirePointer(pointer);
        Objects.requireNonNull(semanticContent, "semanticContent");
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("baseIrVersion", BASE_IR_VERSION);
        projection.put("nodeKind", nodeKind.name());
        projection.put("path", pointer);
        projection.put("semanticContent", semanticContent);
        if (sourceSpan != null) projection.put("sourceSpan", sourceSpan(sourceSpan));
        return AnnotatedIdentity.hashCanonical(projection);
    }

    public static String childPointer(String parent, String segment) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(segment, "segment");
        if (!parent.isEmpty()) requirePointer(parent);
        return parent + "/" + segment.replace("~", "~0").replace("/", "~1");
    }

    private static Map<String, Object> sourceSpan(SourceSpan span) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", span.source());
        result.put("startLine", span.startLine());
        result.put("startColumn", span.startColumn());
        result.put("endLine", span.endLine());
        result.put("endColumn", span.endColumn());
        return result;
    }

    private static void requirePointer(String pointer) {
        Objects.requireNonNull(pointer, "pointer");
        if (pointer.isEmpty() || pointer.charAt(0) != '/') {
            throw new IllegalArgumentException("node path must be an absolute RFC 6901 JSON Pointer");
        }
    }
}
