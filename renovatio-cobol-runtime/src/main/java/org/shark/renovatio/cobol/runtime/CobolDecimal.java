package org.shark.renovatio.cobol.runtime;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A fixed-point numeric value bound to a {@link PicType}, reproducing COBOL
 * store semantics: truncation toward zero to the field scale, optional
 * {@code ROUNDED}, and an {@code ON SIZE ERROR} flag when the integer part
 * does not fit the receiving picture.
 */
public final class CobolDecimal {

    public enum Rounding { TRUNCATE, ROUNDED }

    private final BigDecimal value;
    private final boolean sizeError;

    private CobolDecimal(BigDecimal value, boolean sizeError) {
        this.value = value;
        this.sizeError = sizeError;
    }

    public static CobolDecimal of(PicType type, String value) {
        return store(type, new BigDecimal(value), Rounding.TRUNCATE);
    }

    public BigDecimal value() {
        return value;
    }

    public boolean hasSizeError() {
        return sizeError;
    }

    public CobolDecimal add(CobolDecimal other, PicType resultType, Rounding rounding) {
        return store(resultType, this.value.add(other.value), rounding);
    }

    /** Applies COBOL receiving-field semantics to {@code raw}. */
    private static CobolDecimal store(PicType type, BigDecimal raw, Rounding rounding) {
        RoundingMode mode = rounding == Rounding.ROUNDED ? RoundingMode.HALF_UP : RoundingMode.DOWN;
        BigDecimal scaled = raw.setScale(type.scale(), mode);

        BigDecimal integerPart = scaled.abs().setScale(0, RoundingMode.DOWN);
        boolean sizeError = integerPart.precision() > type.integerDigits()
                && integerPart.compareTo(BigDecimal.ZERO) != 0;

        BigDecimal fitted = scaled;
        if (sizeError) {
            BigDecimal modulus = BigDecimal.TEN.pow(type.integerDigits());
            fitted = scaled.remainder(modulus);
        }
        return new CobolDecimal(fitted, sizeError);
    }
}
