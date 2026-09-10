package org.shark.renovatio.jcl;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.jcl.characterization.BatchCharacterizationHarness;
import org.shark.renovatio.jcl.emit.SpringBatchBatchEmitter;
import org.shark.renovatio.jcl.emit.util.SortUtility;
import org.shark.renovatio.jcl.ir.BatchJobProjection;
import org.shark.renovatio.jcl.parse.JclJob;
import org.shark.renovatio.jcl.parse.JclParser;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.BatchDataset;
import org.shark.renovatio.semantic.ir.BatchJob;
import org.shark.renovatio.semantic.ir.BatchStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class F7AcceptanceTest {
    private static final String JCL = """
            //FLOW JOB
            //ONE EXEC PGM=SORT
            //IN DD DSN=input.dat,DISP=SHR
            //OUT DD DSN=&&TEMP,DISP=(NEW,PASS)
            //TWO EXEC PGM=IEBGENER,COND=(0,NE,ONE)
            //IN DD DSN=&&TEMP,DISP=(OLD,PASS)
            //OUT DD DSN=output.dat,DISP=(NEW,CATLG)
            //THREE EXEC PGM=IDCAMS
            """;

    @Test
    void projectsThreeStepFlowAndEmitsOrderedSpringBatchSource() {
        JclJob parsed = new JclParser().parse("batch/flow.jcl", JCL);
        BatchJob job = new BatchJobProjection().project(parsed, List.of(), "a".repeat(64));
        assertEquals(3, job.steps().size());
        assertEquals(1, job.conditionGraph().guards().size());
        assertEquals(List.of(BatchStep.Kind.STANDARD_UTILITY, BatchStep.Kind.STANDARD_UTILITY,
                BatchStep.Kind.STANDARD_UTILITY), job.steps().stream().map(BatchStep::kind).toList());
        assertTrue(job.datasets().stream().anyMatch(value -> value.access() == BatchDataset.AccessKind.TEMP));

        String source = new SpringBatchBatchEmitter().emit(job, MigrationProfiles.defaults())
                .files().values().iterator().next();
        assertTrue(source.indexOf("one0Step()") < source.indexOf("two1Step()"));
        assertTrue(source.indexOf("two1Step()") < source.indexOf("three2Step()"));
        assertTrue(source.contains("LOAD") || source.contains("ONE.RC NE 0"));
        assertTrue(source.contains("memory:"));
    }

    @Test
    void sortFixtureMatchesReferenceAndCharacterizationDropsTemporaryData() throws Exception {
        List<String> input = List.of("003Ythird", "001Nfirst", "002Ysecond");
        List<String> expected = List.of("002Ysecond", "003Ythird");
        SortUtility sort = new SortUtility();
        assertEquals(expected, sort.execute(input,
                "SORT FIELDS=(1,3,ZD,A) INCLUDE COND=(4,1,CH,EQ,C'Y')"));

        BatchJob job = new BatchJobProjection().project(new JclParser().parse("batch/flow.jcl", JCL),
                List.of(), "b".repeat(64));
        Map<String, List<String>> data = new java.util.LinkedHashMap<>();
        data.put("input.dat", new ArrayList<>(input));
        BatchCharacterizationHarness.RunResult result = new BatchCharacterizationHarness().run(job, data,
                (step, datasets) -> {
                    if (step.stepName().equals("ONE")) datasets.put("&&TEMP", sort.execute(
                            datasets.get("input.dat"), "SORT FIELDS=(1,3,ZD,A) INCLUDE COND=(4,1,CH,EQ,C'Y')"));
                    if (step.stepName().equals("TWO")) datasets.put("output.dat", new ArrayList<>(datasets.get("&&TEMP")));
                    return 0;
                });
        assertEquals(expected, result.datasets().get("output.dat"));
        assertFalse(result.datasets().containsKey("&&TEMP"));
        assertEquals(List.of("ONE", "TWO", "THREE"), result.executedSteps());
    }
}
