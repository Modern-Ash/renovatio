package org.shark.renovatio.cobol.runtime;

/** {@code MOVE} semantics for alphanumeric (DISPLAY text) receiving fields. */
public final class CobolAlphanumeric {

    private CobolAlphanumeric() {
    }

    /**
     * Moves {@code source} into a receiving field of {@code receivingLength}
     * characters: left-justified, space-padded on the right, right-truncated
     * when the source is longer.
     */
    public static String move(String source, int receivingLength) {
        return CobolMove.alphanumeric(source, receivingLength);
    }
}
