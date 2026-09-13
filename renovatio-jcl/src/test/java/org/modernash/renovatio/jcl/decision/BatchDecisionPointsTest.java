package org.modernash.renovatio.jcl.decision;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.decisions.DecisionPoint;
import org.modernash.renovatio.decisions.DecisionSuggestionPort;
import org.modernash.renovatio.jcl.parse.JclStep;

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
                DecisionSuggestionPort.promptId(decision.category()));
    }
}
