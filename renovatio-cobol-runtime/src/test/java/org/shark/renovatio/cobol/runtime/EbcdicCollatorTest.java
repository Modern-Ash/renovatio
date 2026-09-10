package org.shark.renovatio.cobol.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EbcdicCollatorTest {

    private final EbcdicCollator collator = EbcdicCollator.INSTANCE;

    @Test
    void lettersSortBeforeDigits() {
        assertTrue(collator.compare("A", "1") < 0);
    }

    @Test
    void lowercaseSortsBeforeUppercase() {
        assertTrue(collator.compare("a", "A") < 0);
    }

    @Test
    void longerStringSortsAfterItsPrefix() {
        assertTrue(collator.compare("ABC", "AB") > 0);
    }

    @Test
    void spaceSortsBeforeLetters() {
        assertTrue(collator.compare(" ", "A") < 0);
    }
}
