package org.shark.renovatio.domain.model;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Compares observable replay results without depending on either runtime. */
public final class EquivalenceComparator {
    private EquivalenceComparator() { }
    public static Comparison compare(Map<String, ?> baseline, Map<String, ?> candidate, Set<String> ignoredFields) {
        Set<String> ignored = ignoredFields == null ? Set.of() : Set.copyOf(ignoredFields);
        Set<String> keys = new LinkedHashSet<>();
        if (baseline != null) keys.addAll(baseline.keySet());
        if (candidate != null) keys.addAll(candidate.keySet());
        Set<Divergence> divergences = new LinkedHashSet<>();
        for (String key : keys) {
            if (ignored.contains(key)) continue;
            Object left = baseline == null ? null : baseline.get(key);
            Object right = candidate == null ? null : candidate.get(key);
            if (!java.util.Objects.deepEquals(left, right)) divergences.add(new Divergence(key, left, right));
        }
        return new Comparison(divergences.isEmpty(), divergences);
    }
    public record Comparison(boolean equivalent, Set<Divergence> divergences) { }
    public record Divergence(String field, Object baseline, Object candidate) { }
}
