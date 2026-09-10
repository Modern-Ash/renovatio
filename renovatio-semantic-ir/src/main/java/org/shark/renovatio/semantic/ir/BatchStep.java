package org.shark.renovatio.semantic.ir;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** One target-neutral batch step. */
public record BatchStep(String id, String stepName, int ordinal, Kind kind,
                        Optional<String> programRef, Optional<String> utility,
                        List<String> datasetRefs, Optional<String> residueReason) {
    public BatchStep {
        id = SemanticIdentity.hash(id, "id");
        stepName = SemanticIdentity.text(stepName, "stepName").toUpperCase(Locale.ROOT);
        if (ordinal < 0) throw new IllegalArgumentException("ordinal must not be negative");
        Objects.requireNonNull(kind, "kind");
        programRef = clean(programRef, "programRef");
        utility = clean(utility, "utility").map(value -> value.toUpperCase(Locale.ROOT));
        datasetRefs = datasetRefs == null ? List.of() : List.copyOf(datasetRefs);
        datasetRefs.forEach(value -> SemanticIdentity.hash(value, "datasetRef"));
        residueReason = clean(residueReason, "residueReason");
        if (kind == Kind.MIGRATED_PROGRAM_CALL && programRef.isEmpty())
            throw new IllegalArgumentException("migrated program step requires programRef");
        if (kind == Kind.STANDARD_UTILITY && utility.isEmpty())
            throw new IllegalArgumentException("utility step requires utility");
        if (kind == Kind.RESIDUE && residueReason.isEmpty())
            throw new IllegalArgumentException("residue step requires a reason");
    }

    private static Optional<String> clean(Optional<String> value, String name) {
        return value == null ? Optional.empty() : value.map(item -> SemanticIdentity.text(item, name));
    }

    public enum Kind { MIGRATED_PROGRAM_CALL, STANDARD_UTILITY, RESIDUE }
}
