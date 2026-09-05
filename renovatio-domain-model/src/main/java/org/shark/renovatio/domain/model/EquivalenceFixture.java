package org.shark.renovatio.domain.model;

import java.util.Map;
import java.util.Set;

/** Typed replay envelope shared by baseline and target runners. */
public record EquivalenceFixture(String caseId, Map<String, ?> input,
                                 Map<String, ?> baseline, Map<String, ?> candidate,
                                 Set<String> ignoredFields) {
    public EquivalenceFixture {
        if (caseId == null || caseId.isBlank()) throw new IllegalArgumentException("caseId is required");
        input = input == null ? Map.of() : Map.copyOf(input);
        baseline = baseline == null ? Map.of() : Map.copyOf(baseline);
        candidate = candidate == null ? Map.of() : Map.copyOf(candidate);
        ignoredFields = ignoredFields == null ? Set.of() : Set.copyOf(ignoredFields);
    }
    public EquivalenceComparator.Comparison compare() {
        return EquivalenceComparator.compare(baseline, candidate, ignoredFields);
    }
}
