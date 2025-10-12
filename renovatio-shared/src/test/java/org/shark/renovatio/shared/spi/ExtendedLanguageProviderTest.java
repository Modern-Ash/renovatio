package org.shark.renovatio.shared.spi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedLanguageProviderTest {

    @Test
    void default_executeExtendedTool_returnsNull() {
        ExtendedLanguageProvider p = new ExtendedLanguageProvider() {};
        assertNull(p.executeExtendedTool("any", Map.of("k","v")));
    }
}

