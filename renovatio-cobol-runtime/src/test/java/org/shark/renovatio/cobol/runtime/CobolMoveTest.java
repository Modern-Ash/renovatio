package org.shark.renovatio.cobol.runtime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobolMoveTest {

    @Test
    void movesTextWithCobolPaddingAndTruncation() {
        assertEquals("AB   ", CobolMove.alphanumeric("AB", 5));
        assertEquals("ABC", CobolMove.alphanumeric("ABCDEF", 3));
    }

    @Test
    void movesNumericValueWithReceivingScaleTruncation() {
        CobolDecimal result = CobolMove.numeric("12.39", PicClause.parse("S9(2)V9"));

        assertEquals(new BigDecimal("12.3"), result.value());
        assertFalse(result.hasSizeError());
    }

    @Test
    void reportsSizeErrorWhenNumericMoveExceedsReceivingDigits() {
        CobolDecimal result = CobolMove.numeric("1234", PicClause.parse("9(3)"));

        assertEquals(new BigDecimal("234"), result.value());
        assertTrue(result.hasSizeError());
    }
}
