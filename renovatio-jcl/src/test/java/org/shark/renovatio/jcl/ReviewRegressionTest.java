package org.shark.renovatio.jcl;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.jcl.characterization.BatchCharacterizationHarness;
import org.shark.renovatio.jcl.emit.SpringBatchBatchEmitter;
import org.shark.renovatio.jcl.ir.BatchJobProjection;
import org.shark.renovatio.jcl.parse.JclParser;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.BatchDataset;
import org.shark.renovatio.semantic.ir.BatchJob;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewRegressionTest {
    @Test
    void reusesTemporaryIdentityAndPreservesInlineAndConcatenatedResources() {
        BatchJob job = project("""
                //DATAJOB JOB
                //WRITE EXEC PGM=SORT
                //SYSIN DD *
                 SORT FIELDS=(1,3,CH,A)
                 INCLUDE COND=(4,1,CH,EQ,C'Y')
                /*
                //OUT DD DSN=&&TEMP,DISP=(NEW,PASS)
                //READ EXEC PGM=IEBGENER
                //IN DD DSN=&&TEMP,DISP=(OLD,PASS)
                //FILES DD DSN=FIRST.DATA,DISP=SHR
                // DD DSN=SECOND.DATA,DISP=SHR
                """);

        BatchDataset sysin = job.datasets().stream().filter(dataset -> dataset.ddName().equals("SYSIN"))
                .findFirst().orElseThrow();
        assertEquals(List.of(" SORT FIELDS=(1,3,CH,A)", " INCLUDE COND=(4,1,CH,EQ,C'Y')"),
                sysin.inlineRecords());
        BatchDataset files = job.datasets().stream().filter(dataset -> dataset.ddName().equals("FILES"))
                .findFirst().orElseThrow();
        assertEquals("SECOND.DATA", files.concatenations().get(0).resourceReference().orElseThrow());

        String source = emit(job);
        assertEquals(2, occurrences(source, "memory:&&TEMP"));
        String encoded = Base64.getEncoder().encodeToString(String.join("\n", sysin.inlineRecords())
                .getBytes(StandardCharsets.UTF_8));
        assertTrue(source.contains("inline-base64:" + encoded));
        assertTrue(source.contains("java.util.List.of(\"FIRST.DATA\", \"SECOND.DATA\")"));
    }

    @Test
    void emittedTaskletsPersistReturnCodesForFollowingGuards() {
        String source = emit(project("""
                //RCJOB JOB
                //ONE EXEC PGM=IEBGENER
                //TWO EXEC PGM=SORT,COND=(0,NE,ONE)
                """));

        assertTrue(source.contains("int returnCode = runtime.utility"));
        assertTrue(source.contains("putInt(\"ONE.RC\", returnCode)"));
        assertTrue(source.contains("new ExitStatus(\"RC=\" + returnCode)"));
        assertTrue(source.contains("int run(String program"));
        assertTrue(source.contains("int utility(String utility"));
    }

    @Test
    void characterizationEvaluatesThenAndElseGuards() throws Exception {
        BatchJob job = project("""
                //IFJOB JOB
                //BASE EXEC PGM=BASE
                // IF (BASE.RC = 0) THEN
                //YES EXEC PGM=YES
                // ELSE
                //NO EXEC PGM=NO
                // ENDIF
                """);

        BatchCharacterizationHarness.RunResult result = new BatchCharacterizationHarness().run(job, Map.of(),
                (step, datasets) -> 0);

        assertEquals(List.of("BASE", "YES"), result.executedSteps());
    }

    @Test
    void compoundCondUsesEveryReferencedStepAtRuntime() throws Exception {
        BatchJob job = project("""
                //CONDJOB JOB
                //S1 EXEC PGM=P1
                //S2 EXEC PGM=P2
                //S3 EXEC PGM=P3,COND=((0,NE,S1),(4,LT,S2))
                """);

        assertEquals(2, job.conditionGraph().guards().size());
        assertEquals(2, job.conditionGraph().guards().stream()
                .map(guard -> guard.referencedStepId().orElseThrow()).distinct().count());
        Map<String, Integer> codes = Map.of("S1", 0, "S2", 0);
        BatchCharacterizationHarness.RunResult result = new BatchCharacterizationHarness().run(job,
                new LinkedHashMap<>(), (step, datasets) -> codes.getOrDefault(step.stepName(), 0));

        assertEquals(List.of("S1", "S2"), result.executedSteps());
    }

    private static BatchJob project(String jcl) {
        return new BatchJobProjection().project(new JclParser().parse("batch/review.jcl", jcl),
                List.of(), "c".repeat(64));
    }

    private static String emit(BatchJob job) {
        return new SpringBatchBatchEmitter().emit(job, MigrationProfiles.defaults())
                .files().values().iterator().next();
    }

    private static int occurrences(String text, String value) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(value, index)) >= 0; index += value.length()) count++;
        return count;
    }
}
