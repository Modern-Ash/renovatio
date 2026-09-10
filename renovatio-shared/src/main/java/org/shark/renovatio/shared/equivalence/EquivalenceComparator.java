package org.shark.renovatio.shared.equivalence;

import java.util.Map;
import java.util.TreeMap;

/** Deterministic comparison of already-observed COBOL and target evidence. */
public final class EquivalenceComparator {
    public Result compare(String sourceHash, String targetHash, Map<String, String> sourceInvariants,
                          Map<String, String> targetInvariants, boolean intentionalChange) {
        if (blank(sourceHash) || blank(targetHash)) return new Result(EquivalenceClassification.UNDETERMINED, "missing-hash");
        if (!new TreeMap<>(sourceInvariants == null ? Map.of() : sourceInvariants).equals(new TreeMap<>(targetInvariants == null ? Map.of() : targetInvariants)))
            return new Result(intentionalChange ? EquivalenceClassification.INTENTIONAL_CHANGE : EquivalenceClassification.REGRESSION, "invariant-delta");
        return new Result(sourceHash.equals(targetHash) ? EquivalenceClassification.EQUIVALENT : (intentionalChange ? EquivalenceClassification.INTENTIONAL_CHANGE : EquivalenceClassification.REGRESSION), sourceHash.equals(targetHash) ? "hash-match" : "hash-delta");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    public record Result(EquivalenceClassification classification, String reason) { }
}
