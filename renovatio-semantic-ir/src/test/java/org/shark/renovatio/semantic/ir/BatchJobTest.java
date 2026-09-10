package org.shark.renovatio.semantic.ir;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BatchJobTest {
    @Test
    void ordersNodesDefensivelyAndRejectsDanglingReferences() {
        String firstId = BatchJob.stepId("job", "FIRST", 0);
        String secondId = BatchJob.stepId("job", "SECOND", 1);
        String datasetId = BatchJob.datasetId("job", "FIRST", "INPUT");
        BatchStep first = new BatchStep(firstId, "FIRST", 0, BatchStep.Kind.MIGRATED_PROGRAM_CALL,
                Optional.of("PROGRAM1"), Optional.empty(), List.of(datasetId), Optional.empty());
        BatchStep second = new BatchStep(secondId, "SECOND", 1, BatchStep.Kind.STANDARD_UTILITY,
                Optional.empty(), Optional.of("SORT"), List.of(), Optional.empty());
        BatchDataset dataset = new BatchDataset(datasetId, "INPUT", BatchDataset.AccessKind.SEQ_IN,
                Optional.of("input.dat"));
        LinkedHashMap<String, Boolean> table = new LinkedHashMap<>();
        table.put("FIRST.RC=0", true);
        table.put("FIRST.RC=4", false);
        BatchJob job = new BatchJob("1", "job", provenance(), List.of(second, first), List.of(dataset),
                new ConditionGraph(List.of(new ConditionGraph.Guard("FIRST.RC NE 0", Optional.of(firstId),
                        table, List.of(secondId)))));

        assertEquals(List.of("FIRST", "SECOND"), job.steps().stream().map(BatchStep::stepName).toList());
        assertEquals("JOB", job.jobId());
        assertThrows(UnsupportedOperationException.class, () -> job.steps().add(first));
        assertEquals(firstId, BatchJob.stepId("JOB", "first", 0));

        BatchStep dangling = new BatchStep(firstId, "FIRST", 0, BatchStep.Kind.MIGRATED_PROGRAM_CALL,
                Optional.of("PROGRAM1"), Optional.empty(), List.of("f".repeat(64)), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new BatchJob("1", "JOB", provenance(),
                List.of(dangling), List.of(), new ConditionGraph(List.of())));
    }

    private static SourceProvenance provenance() {
        return new SourceProvenance("batch/job.jcl", "0".repeat(64), "JCL", Optional.of("IBM"), List.of());
    }
}
