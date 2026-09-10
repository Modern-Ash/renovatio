package org.shark.renovatio.cobol.runtime;

/** COBOL {@code MOVE} semantics for elementary receiving fields. */
public final class CobolMove {

    private CobolMove() {
    }

    /**
     * Moves text into a fixed-width alphanumeric receiver: left-justified,
     * space-padded on the right, and truncated on the right when necessary.
     */
    public static String alphanumeric(String source, int receivingLength) {
        if (receivingLength < 0) {
            throw new IllegalArgumentException("receivingLength must be >= 0");
        }
        String value = source == null ? "" : source;
        if (value.length() >= receivingLength) {
            return value.substring(0, receivingLength);
        }
        return value + " ".repeat(receivingLength - value.length());
    }

    /**
     * Moves a numeric value into a receiving picture using its scale,
     * truncation, and size-error rules.
     */
    public static CobolDecimal numeric(String source, PicType receivingType) {
        if (receivingType == null || receivingType.category() != PicType.Category.NUMERIC) {
            throw new IllegalArgumentException("receivingType must be numeric");
        }
        return CobolDecimal.of(receivingType, source);
    }
}
