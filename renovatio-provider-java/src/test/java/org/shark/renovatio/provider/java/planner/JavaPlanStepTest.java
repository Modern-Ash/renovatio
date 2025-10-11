package org.shark.renovatio.provider.java.planner;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaPlanStepTest {

    @Test
    void toMap_containsAllFields() {
        JavaPlanStep step = new JavaPlanStep("s1", "r1", "desc");
        Map<String, Object> map = step.toMap();
        assertEquals("s1", map.get("id"));
        assertEquals("r1", map.get("recipe"));
        assertEquals("desc", map.get("description"));
    }
}

