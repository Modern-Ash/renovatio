package org.shark.renovatio.shared.equivalence;

import java.util.Map;
import java.util.TreeMap;

/** Compares the named business invariants selected by a fixture contract. */
public final class BusinessRuleEquivalence {
    private BusinessRuleEquivalence() { }

    public static Map<String, String> violations(Map<String, String> source, Map<String, String> target,
                                                  java.util.Set<String> requiredRules) {
        Map<String, String> result = new TreeMap<>();
        for (String rule : new java.util.TreeSet<>(requiredRules == null ? java.util.Set.of() : requiredRules)) {
            String expected = source == null ? null : source.get(rule);
            String actual = target == null ? null : target.get(rule);
            if (!java.util.Objects.equals(expected, actual)) result.put(rule, String.valueOf(expected) + " -> " + String.valueOf(actual));
        }
        return Map.copyOf(result);
    }
}
