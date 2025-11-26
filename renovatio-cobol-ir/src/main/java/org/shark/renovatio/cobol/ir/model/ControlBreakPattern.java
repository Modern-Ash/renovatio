package org.shark.renovatio.cobol.ir.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a control break pattern detected in COBOL code.
 * Control break patterns occur when processing sequential/ISAM files with grouping logic,
 * where the program detects changes in key fields to trigger subtotal/group processing.
 *
 * <p>This pattern is common in COBOL batch programs and does not translate directly to
 * modern architectures. Renovatio uses this model to decompose the pattern into:
 * <ul>
 *   <li>Data access layer (repository pattern)</li>
 *   <li>Business logic (service/strategy patterns)</li>
 *   <li>Aggregation logic (stream-based or collector patterns)</li>
 * </ul>
 */
public record ControlBreakPattern(
        String patternId,
        String fileName,
        List<BreakLevel> breakLevels,
        List<CobolStatement> initializationStatements,
        List<CobolStatement> detailProcessingStatements,
        List<CobolStatement> finalizationStatements,
        FileOperationType fileOperationType
) {

    public ControlBreakPattern {
        Objects.requireNonNull(patternId, "patternId");
        Objects.requireNonNull(fileName, "fileName");
        breakLevels = breakLevels == null ? List.of() : Collections.unmodifiableList(breakLevels);
        initializationStatements = initializationStatements == null ? List.of() : Collections.unmodifiableList(initializationStatements);
        detailProcessingStatements = detailProcessingStatements == null ? List.of() : Collections.unmodifiableList(detailProcessingStatements);
        finalizationStatements = finalizationStatements == null ? List.of() : Collections.unmodifiableList(finalizationStatements);
        fileOperationType = fileOperationType == null ? FileOperationType.SEQUENTIAL : fileOperationType;
    }

    /**
     * Type of file operation in the control break pattern.
     */
    public enum FileOperationType {
        /** Sequential file access (READ NEXT) */
        SEQUENTIAL,
        /** Indexed/ISAM file access */
        INDEXED,
        /** VSAM KSDS file access */
        VSAM_KSDS,
        /** VSAM RRDS file access */
        VSAM_RRDS
    }

    /**
     * Represents a single break level in the control break hierarchy.
     * Break levels are ordered from major (level 1) to minor (higher levels).
     */
    public record BreakLevel(
            int level,
            String controlField,
            String previousValueField,
            List<CobolStatement> breakStatements,
            List<AggregationField> aggregations
    ) {
        public BreakLevel {
            Objects.requireNonNull(controlField, "controlField");
            breakStatements = breakStatements == null ? List.of() : Collections.unmodifiableList(breakStatements);
            aggregations = aggregations == null ? List.of() : Collections.unmodifiableList(aggregations);
        }
    }

    /**
     * Represents an aggregation field used in control break processing.
     */
    public record AggregationField(
            String fieldName,
            AggregationType aggregationType,
            String sourceField
    ) {
        public AggregationField {
            Objects.requireNonNull(fieldName, "fieldName");
            Objects.requireNonNull(aggregationType, "aggregationType");
        }
    }

    /**
     * Types of aggregations commonly found in control break logic.
     */
    public enum AggregationType {
        SUM,
        COUNT,
        AVERAGE,
        MIN,
        MAX,
        FIRST,
        LAST,
        ACCUMULATOR
    }

    /**
     * Builder for creating ControlBreakPattern instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String patternId;
        private String fileName;
        private List<BreakLevel> breakLevels;
        private List<CobolStatement> initializationStatements;
        private List<CobolStatement> detailProcessingStatements;
        private List<CobolStatement> finalizationStatements;
        private FileOperationType fileOperationType;

        private Builder() {
        }

        public Builder patternId(String patternId) {
            this.patternId = patternId;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder breakLevels(List<BreakLevel> breakLevels) {
            this.breakLevels = breakLevels;
            return this;
        }

        public Builder initializationStatements(List<CobolStatement> statements) {
            this.initializationStatements = statements;
            return this;
        }

        public Builder detailProcessingStatements(List<CobolStatement> statements) {
            this.detailProcessingStatements = statements;
            return this;
        }

        public Builder finalizationStatements(List<CobolStatement> statements) {
            this.finalizationStatements = statements;
            return this;
        }

        public Builder fileOperationType(FileOperationType type) {
            this.fileOperationType = type;
            return this;
        }

        public ControlBreakPattern build() {
            return new ControlBreakPattern(
                    patternId,
                    fileName,
                    breakLevels,
                    initializationStatements,
                    detailProcessingStatements,
                    finalizationStatements,
                    fileOperationType
            );
        }
    }
}
