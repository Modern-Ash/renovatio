package org.shark.renovatio.jcl.decision;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.jcl.parse.JclStep;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BatchDecisionPointsTest {
    @Test
    void ambiguousStepProducesBatchSuggestionThatDefaultsToResidue() {
        JclStep step = new JclStep("MYSTEP", JclStep.ExecKind.PROGRAM, "UNKNOWN", Optional.empty(),
                Optional.empty(), List.of(), Map.of(), 1);
        DecisionPoint decision = BatchDecisionPoints.ambiguousStep("JOB", step, "0".repeat(64), Instant.EPOCH);
        assertEquals(DecisionPoint.Category.BATCH, decision.category());
        assertEquals("RESIDUE", decision.chosenOption());
        assertEquals("decision.batch.v1",
                org.shark.renovatio.llm.decision.DecisionSuggestionService.promptId(decision.category()));
    }
}
