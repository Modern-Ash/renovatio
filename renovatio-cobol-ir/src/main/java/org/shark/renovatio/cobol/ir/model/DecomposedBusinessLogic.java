package org.shark.renovatio.cobol.ir.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the result of decomposing a COBOL program's control break patterns
 * into reusable architectural components.
 *
 * <p>This model separates the concerns of COBOL file processing programs into:
 * <ul>
 *   <li><b>Data Access</b>: File/record access operations → Repository pattern</li>
 *   <li><b>Business Logic</b>: Field transformations and calculations → Service methods</li>
 *   <li><b>Aggregation Logic</b>: Grouping and totaling → Stream collectors or strategies</li>
 *   <li><b>Control Flow</b>: Break level handling → State machine or event-driven design</li>
 * </ul>
 *
 * <p>This decomposition solves the architectural impedance mismatch by extracting
 * the reusable business logic from the file-processing scaffolding.
 */
public record DecomposedBusinessLogic(
        String programId,
        List<DataAccessComponent> dataAccessComponents,
        List<BusinessRuleComponent> businessRules,
        List<AggregationComponent> aggregations,
        List<ValidationComponent> validations,
        Map<String, Object> metadata
) {

    public DecomposedBusinessLogic {
        Objects.requireNonNull(programId, "programId");
        dataAccessComponents = dataAccessComponents == null ? List.of() : Collections.unmodifiableList(dataAccessComponents);
        businessRules = businessRules == null ? List.of() : Collections.unmodifiableList(businessRules);
        aggregations = aggregations == null ? List.of() : Collections.unmodifiableList(aggregations);
        validations = validations == null ? List.of() : Collections.unmodifiableList(validations);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    /**
     * Represents a data access component extracted from file operations.
     */
    public record DataAccessComponent(
            String componentId,
            String entityName,
            String recordName,
            List<String> keyFields,
            List<FieldMapping> fieldMappings,
            AccessPattern accessPattern
    ) {
        public DataAccessComponent {
            Objects.requireNonNull(componentId, "componentId");
            Objects.requireNonNull(entityName, "entityName");
            keyFields = keyFields == null ? List.of() : Collections.unmodifiableList(keyFields);
            fieldMappings = fieldMappings == null ? List.of() : Collections.unmodifiableList(fieldMappings);
            accessPattern = accessPattern == null ? AccessPattern.READ_ALL : accessPattern;
        }

        public enum AccessPattern {
            READ_ALL,
            READ_BY_KEY,
            READ_SEQUENTIAL,
            READ_WITH_FILTER,
            CRUD
        }
    }

    /**
     * Represents a field mapping from COBOL to target platform.
     */
    public record FieldMapping(
            String cobolFieldName,
            String targetFieldName,
            String cobolType,
            String targetType,
            String transformationExpression
    ) {
        public FieldMapping {
            Objects.requireNonNull(cobolFieldName, "cobolFieldName");
            Objects.requireNonNull(targetFieldName, "targetFieldName");
        }
    }

    /**
     * Represents extracted business rules/logic.
     */
    public record BusinessRuleComponent(
            String ruleId,
            String ruleName,
            String description,
            RuleType ruleType,
            List<String> inputFields,
            List<String> outputFields,
            String expression,
            List<CobolStatement> originalStatements
    ) {
        public BusinessRuleComponent {
            Objects.requireNonNull(ruleId, "ruleId");
            Objects.requireNonNull(ruleName, "ruleName");
            inputFields = inputFields == null ? List.of() : Collections.unmodifiableList(inputFields);
            outputFields = outputFields == null ? List.of() : Collections.unmodifiableList(outputFields);
            originalStatements = originalStatements == null ? List.of() : Collections.unmodifiableList(originalStatements);
            ruleType = ruleType == null ? RuleType.TRANSFORMATION : ruleType;
        }

        public enum RuleType {
            TRANSFORMATION,
            CALCULATION,
            CONDITIONAL,
            ASSIGNMENT,
            FORMATTING,
            LOOKUP
        }
    }

    /**
     * Represents aggregation/grouping logic extracted from control breaks.
     */
    public record AggregationComponent(
            String aggregationId,
            String name,
            List<String> groupByFields,
            List<AggregationOperation> operations,
            int breakLevel
    ) {
        public AggregationComponent {
            Objects.requireNonNull(aggregationId, "aggregationId");
            Objects.requireNonNull(name, "name");
            groupByFields = groupByFields == null ? List.of() : Collections.unmodifiableList(groupByFields);
            operations = operations == null ? List.of() : Collections.unmodifiableList(operations);
        }
    }

    /**
     * Represents a single aggregation operation.
     */
    public record AggregationOperation(
            String operationName,
            OperationType operationType,
            String sourceField,
            String targetField
    ) {
        public AggregationOperation {
            Objects.requireNonNull(operationName, "operationName");
            Objects.requireNonNull(operationType, "operationType");
        }

        public enum OperationType {
            SUM,
            COUNT,
            AVERAGE,
            MIN,
            MAX,
            COLLECT,
            GROUP_BY,
            PARTITION_BY
        }
    }

    /**
     * Represents validation rules extracted from the program.
     */
    public record ValidationComponent(
            String validationId,
            String fieldName,
            String condition,
            String errorMessage,
            ValidationType validationType
    ) {
        public ValidationComponent {
            Objects.requireNonNull(validationId, "validationId");
            Objects.requireNonNull(fieldName, "fieldName");
            validationType = validationType == null ? ValidationType.REQUIRED : validationType;
        }

        public enum ValidationType {
            REQUIRED,
            RANGE,
            FORMAT,
            LOOKUP,
            CUSTOM
        }
    }

    /**
     * Builder for creating DecomposedBusinessLogic instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String programId;
        private List<DataAccessComponent> dataAccessComponents;
        private List<BusinessRuleComponent> businessRules;
        private List<AggregationComponent> aggregations;
        private List<ValidationComponent> validations;
        private Map<String, Object> metadata;

        private Builder() {
        }

        public Builder programId(String programId) {
            this.programId = programId;
            return this;
        }

        public Builder dataAccessComponents(List<DataAccessComponent> components) {
            this.dataAccessComponents = components;
            return this;
        }

        public Builder businessRules(List<BusinessRuleComponent> rules) {
            this.businessRules = rules;
            return this;
        }

        public Builder aggregations(List<AggregationComponent> aggregations) {
            this.aggregations = aggregations;
            return this;
        }

        public Builder validations(List<ValidationComponent> validations) {
            this.validations = validations;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public DecomposedBusinessLogic build() {
            return new DecomposedBusinessLogic(
                    programId,
                    dataAccessComponents,
                    businessRules,
                    aggregations,
                    validations,
                    metadata
            );
        }
    }
}
