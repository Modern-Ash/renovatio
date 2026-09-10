package org.shark.renovatio.cobol.runtime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobolDecimalTest {

    private static PicType pic(String p) {
        return PicClause.parse(p);
    }

    @Test
    void constructionTruncatesTowardZeroToScale() {
        CobolDecimal d = CobolDecimal.of(pic("S9(3)V99"), "10.005");
        assertEquals(new BigDecimal("10.00"), d.value());
    }

    @Test
    void constructionTruncatesNegativeTowardZero() {
        CobolDecimal d = CobolDecimal.of(pic("S9(3)V99"), "-1.009");
        assertEquals(new BigDecimal("-1.00"), d.value());
    }

    @Test
    void addWithTruncation() {
        CobolDecimal a = CobolDecimal.of(pic("9(3)V99"), "1.11");
        CobolDecimal b = CobolDecimal.of(pic("9(3)V99"), "2.226");
        CobolDecimal r = a.add(b, pic("9(3)V9"), CobolDecimal.Rounding.TRUNCATE);
        assertEquals(new BigDecimal("3.3"), r.value());
        assertFalse(r.hasSizeError());
    }

    @Test
    void addWithRounding() {
        CobolDecimal a = CobolDecimal.of(pic("9(3)V99"), "1.11");
        CobolDecimal b = CobolDecimal.of(pic("9(3)V99"), "2.25");
        CobolDecimal r = a.add(b, pic("9(3)V9"), CobolDecimal.Rounding.ROUNDED);
        assertEquals(new BigDecimal("3.4"), r.value());
    }

    @Test
    void sizeErrorOnIntegerOverflowTruncatesHighOrder() {
        CobolDecimal a = CobolDecimal.of(pic("9(2)"), "80");
        CobolDecimal b = CobolDecimal.of(pic("9(2)"), "50");
        CobolDecimal r = a.add(b, pic("9(2)"), CobolDecimal.Rounding.TRUNCATE);
        assertTrue(r.hasSizeError());
        assertEquals(new BigDecimal("30"), r.value());
    }

    @Test
    void sizeErrorUsesIntegerDigitsWhenReceiverHasFractionalScale() {
        CobolDecimal result = CobolDecimal.of(pic("9(2)V99"), "123.45");

        assertTrue(result.hasSizeError());
        assertEquals(new BigDecimal("23.45"), result.value());
    }
}
