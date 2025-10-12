package org.shark.renovatio.provider.java.planner;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaPlanTest {

    @Test
    void record_fields_areAccessible() {
        Instant now = Instant.now();
        JavaPlan plan = new JavaPlan("p1", "/ws", List.of("goal"), List.of("r"), List.of("**/*"), List.of(new JavaPlanStep("s","r","d")), now);
        assertEquals("p1", plan.id());
        assertEquals("/ws", plan.workspacePath());
        assertEquals(List.of("goal"), plan.goals());
        assertEquals(List.of("r"), plan.recipes());
        assertEquals(List.of("**/*"), plan.scope());
        assertEquals(1, plan.steps().size());
        assertEquals(now, plan.createdAt());
    }
}

