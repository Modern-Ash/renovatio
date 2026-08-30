package org.shark.renovatio.cobol.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolAlphanumericTest {

    @Test
    void padsShorterSourceWithTrailingSpaces() {
        assertEquals("AB   ", CobolAlphanumeric.move("AB", 5));
    }

    @Test
    void truncatesLongerSourceOnTheRight() {
        assertEquals("ABC", CobolAlphanumeric.move("ABCDEF", 3));
    }

    @Test
    void fillsEntireFieldWhenSourceEmpty() {
        assertEquals("   ", CobolAlphanumeric.move("", 3));
    }
}
