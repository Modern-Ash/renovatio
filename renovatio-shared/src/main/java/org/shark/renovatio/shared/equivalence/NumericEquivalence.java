package org.shark.renovatio.shared.equivalence;

import java.math.BigDecimal;

/** COBOL-compatible numeric comparison: leading zeros and scale do not change value. */
public final class NumericEquivalence {
    private NumericEquivalence() { }
    public static boolean equivalent(String left, String right) {
        try { return new BigDecimal(left.trim()).compareTo(new BigDecimal(right.trim())) == 0; }
        catch (RuntimeException ignored) { return false; }
    }
}
