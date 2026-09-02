package org.shark.renovatio.persistence.classifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Classification of a single data access detected by the F2 semantic IR.
 * Immutable, null-safe, deterministic ordering.
 */
public record DataAccessClassification(
    String id,
    String sourceId,
    DataAccessKind kind,
    Optional<String> resourceReference,
    KeyShape keyShape,
    RecordShape recordShape,
    Optional<String> discriminatorField,
    List<DiscriminatorValue> discriminatorValues,
    double confidence,
    List<String> evidenceIds,
    ClassifierProvenance provenance
) {
    public DataAccessClassification {
        id = Objects.requireNonNull(id, "id");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(kind, "kind");
        resourceReference = resourceReference == null ? Optional.empty() : resourceReference.map(v -> v.isBlank() ? null : v).filter(v -> v != null);
        Objects.requireNonNull(keyShape, "keyShape");
        Objects.requireNonNull(recordShape, "recordShape");
        discriminatorField = discriminatorField == null ? Optional.empty() : discriminatorField.map(v -> v.isBlank() ? null : v).filter(v -> v != null);
        discriminatorValues = discriminatorValues == null ? List.of() : List.copyOf(discriminatorValues);
        if (confidence < 0.0 || confidence > 1.0) throw new IllegalArgumentException("confidence must be 0.0-1.0");
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        Objects.requireNonNull(provenance, "provenance");
    }

    public record KeyShape(List<String> fields) {
        public static final KeyShape NONE = new KeyShape(List.of());
        public KeyShape {
            fields = fields == null ? List.of() : List.copyOf(fields);
        }
        public boolean isNone() { return fields.isEmpty(); }
    }

    public record RecordShape(String fdName, Optional<String> table, List<String> columns) {
        public static final RecordShape UNKNOWN = new RecordShape(null, Optional.empty(), List.of());
        public RecordShape {
            fdName = fdName; // nullable by design
            table = table == null ? Optional.empty() : table;
            columns = columns == null ? List.of() : List.copyOf(columns);
        }
        public boolean isUnknown() { return fdName == null && table.isEmpty() && columns.isEmpty(); }
    }

    public record DiscriminatorValue(String flag, String layoutName) {
        public DiscriminatorValue {
            flag = Objects.requireNonNull(flag, "flag");
            layoutName = Objects.requireNonNull(layoutName, "layoutName");
        }
    }

    public record ClassifierProvenance(String sourcePath, String inputHash, String sourceLanguage) {
        public ClassifierProvenance {
            sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
            inputHash = Objects.requireNonNull(inputHash, "inputHash");
            sourceLanguage = Objects.requireNonNull(sourceLanguage, "sourceLanguage");
        }
    }
}
