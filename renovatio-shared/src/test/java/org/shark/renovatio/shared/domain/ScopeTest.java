package org.shark.renovatio.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTest {

    @Test
    void getters_and_setters_and_constructor() {
        Scope s = new Scope();
        s.setPaths(List.of("a","b"));
        s.setIncludePatterns(List.of("**/*.java"));
        s.setExcludePatterns(List.of("**/test/**"));
        s.setProperties(Map.of("k","v"));

        assertEquals(List.of("a","b"), s.getPaths());
        assertEquals(List.of("**/*.java"), s.getIncludePatterns());
        assertEquals(List.of("**/test/**"), s.getExcludePatterns());
        assertEquals("v", s.getProperties().get("k"));

        Scope s2 = new Scope(List.of("root"));
        assertEquals(List.of("root"), s2.getPaths());
    }
}

