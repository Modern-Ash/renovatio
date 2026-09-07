package org.shark.renovatio.shared.equivalence;

import java.util.Map;
import java.util.TreeMap;

/** Stable evidence emitted per characterization fixture. */
public record EquivalenceReport(String fixtureId, String sourceHash, String targetHash,
                                Map<String, String> sourceInvariants, Map<String, String> targetInvariants,
                                EquivalenceClassification classification, String reason) {
    public EquivalenceReport {
        if (fixtureId == null || fixtureId.isBlank()) throw new IllegalArgumentException("fixtureId is required");
        sourceInvariants = stable(sourceInvariants);
        targetInvariants = stable(targetInvariants);
    }
    private static Map<String, String> stable(Map<String, String> values) {
        return Map.copyOf(new TreeMap<>(values == null ? Map.of() : values));
    }
    public boolean blocksRelease() { return classification == EquivalenceClassification.REGRESSION || classification == EquivalenceClassification.UNDETERMINED; }
}
