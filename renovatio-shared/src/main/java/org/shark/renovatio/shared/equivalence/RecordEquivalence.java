package org.shark.renovatio.shared.equivalence;

import java.util.Map;
import java.util.TreeMap;

/** Exact record comparison with stable field ordering for fixture diagnostics. */
public final class RecordEquivalence {
    private RecordEquivalence() { }

    public static boolean equivalent(Map<String, String> source, Map<String, String> target) {
        return stable(source).equals(stable(target));
    }

    public static Map<String, String> differences(Map<String, String> source, Map<String, String> target) {
        Map<String, String> result = new TreeMap<>();
        Map<String, String> left = stable(source); Map<String, String> right = stable(target);
        java.util.Set<String> fields = new java.util.TreeSet<>(); fields.addAll(left.keySet()); fields.addAll(right.keySet());
        for (String field : fields) if (!java.util.Objects.equals(left.get(field), right.get(field)))
            result.put(field, String.valueOf(left.get(field)) + " -> " + String.valueOf(right.get(field)));
        return Map.copyOf(result);
    }

    private static Map<String, String> stable(Map<String, String> values) { return new TreeMap<>(values == null ? Map.of() : values); }
}
