package org.shark.renovatio.provider.java.execution;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaRecipeExecutionResultTest {

    @Test
    void record_fields_areAccessible() {
        JavaRecipeExecutionResult r = new JavaRecipeExecutionResult(true, true,
                List.of(new JavaChange("f","d")), List.of(Map.of("k","v")), Map.of("m",1),
                List.of("F.java"), 42L, List.of("R"), "summary");
        assertTrue(r.success());
        assertTrue(r.applied());
        assertEquals(1, r.changes().size());
        assertEquals("v", r.issues().get(0).get("k"));
        assertEquals(1, r.metrics().get("m"));
        assertEquals(List.of("F.java"), r.analyzedFiles());
        assertEquals(42L, r.durationMs());
        assertEquals(List.of("R"), r.recipes());
        assertEquals("summary", r.summary());
    }
}

