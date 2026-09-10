package org.shark.renovatio.cobol.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PicClauseTest {

    @Test
    void parsesSignedPackedDecimalWithScale() {
        PicType t = PicClause.parse("S9(4)V99 COMP-3");

        assertEquals(PicType.Category.NUMERIC, t.category());
        assertEquals(6, t.digits());
        assertEquals(2, t.scale());
        assertEquals(4, t.integerDigits());
        assertTrue(t.signed());
        assertEquals(PicType.Usage.COMP_3, t.usage());
    }

    @Test
    void parsesUnsignedDisplayInteger() {
        PicType t = PicClause.parse("PIC 9(5)");

        assertEquals(PicType.Category.NUMERIC, t.category());
        assertEquals(5, t.digits());
        assertEquals(0, t.scale());
        assertFalse(t.signed());
        assertEquals(PicType.Usage.DISPLAY, t.usage());
    }

    @Test
    void parsesAlphanumericWithExplicitLength() {
        PicType t = PicClause.parse("PIC X(30)");

        assertEquals(PicType.Category.ALPHANUMERIC, t.category());
        assertEquals(30, t.digits());
        assertEquals(0, t.scale());
        assertEquals(PicType.Usage.DISPLAY, t.usage());
    }

    @Test
    void parsesBinaryComp() {
        PicType t = PicClause.parse("9(9) COMP");

        assertEquals(PicType.Usage.COMP, t.usage());
        assertEquals(9, t.digits());
    }

    @Test
    void parsesNativeBinaryCompFive() {
        PicType t = PicClause.parse("S9(9) USAGE COMP-5");

        assertEquals(PicType.Category.NUMERIC, t.category());
        assertEquals(PicType.Usage.COMP_5, t.usage());
        assertTrue(t.signed());
    }

    @Test
    void parsesSeparateSignClauseWithoutTreatingItsWordsAsPictureSymbols() {
        PicType t = PicClause.parse("9(4) SIGN IS LEADING SEPARATE CHARACTER");

        assertEquals(PicType.Category.NUMERIC, t.category());
        assertEquals(4, t.digits());
        assertTrue(t.signed());
    }

    @Test
    void parsesLiteralNineRun() {
        PicType t = PicClause.parse("PIC 999V99");

        assertEquals(5, t.digits());
        assertEquals(2, t.scale());
    }
}
