package org.shark.renovatio.jcl.decision;

import org.shark.renovatio.decisions.DecisionIdentity;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.jcl.parse.JclStep;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Creates bounded BATCH decisions; suggestions remain uncommitted until human confirmation. */
public final class BatchDecisionPoints {
    private BatchDecisionPoints() { }

    public static DecisionPoint target(String semanticIrHash, Instant now) {
        return create("batch.target", DecisionPoint.Location.project(),
                "Which orchestration target should this batch job use?",
                List.of("SPRING_BATCH", "CLI_PIPELINE", "SCHEDULER", "WORKFLOW_ENGINE"),
                "SPRING_BATCH", BigDecimal.ONE, "Java batch compatibility default", semanticIrHash, now);
    }

    public static DecisionPoint ambiguousStep(String jobId, JclStep step, String semanticIrHash, Instant now) {
        DecisionPoint.Location location = new DecisionPoint.Location(jobId, "BATCH_STEP", step.stepName());
        return create("batch.step." + step.stepName() + ".classification", location,
                "How should batch step " + step.stepName() + " be classified?",
                List.of("RESIDUE", "MIGRATED_PROGRAM_CALL", "STANDARD_UTILITY"), "RESIDUE",
                new BigDecimal("0.5"), "Unknown steps remain explicit residue by default",
                semanticIrHash, now);
    }

    private static DecisionPoint create(String key, DecisionPoint.Location location, String question,
                                        List<String> options, String defaultOption, BigDecimal confidence,
                                        String rationale, String semanticIrHash, Instant now) {
        return new DecisionPoint(DecisionPoint.SCHEMA_VERSION,
                DecisionIdentity.id(DecisionPoint.Category.BATCH, key, location),
                DecisionPoint.Category.BATCH, key, location, question, options, defaultOption, defaultOption,
                DecisionPoint.Source.HEURISTIC, confidence, rationale, List.of("F7 batch policy"),
                DecisionPoint.Status.AUTO, semanticIrHash, false, null, 1, now, now, true);
    }
}
