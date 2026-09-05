package org.shark.renovatio.domain.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EquivalenceGateTest {
    @Test void requiresThresholdAndCases() {
        assertTrue(EquivalenceGate.evaluate(10, 10, .99).readyForHumanCutoverReview());
        assertFalse(EquivalenceGate.evaluate(10, 9, .99).readyForHumanCutoverReview());
        assertFalse(EquivalenceGate.evaluate(0, 0, 1).readyForHumanCutoverReview());
    }
}
