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

    @Test
    void flushesPendingStepBeforeStartingAnotherJob() {
        List<JclJob> jobs = new JclParser().parseAll(List.of(new org.shark.renovatio.jcl.parse.JclSource(
                "batch/jobs.jcl", "//ONE JOB\n//A EXEC PGM=A\n//TWO JOB\n//B EXEC PGM=B\n")));
        assertEquals(List.of("A"), jobs.get(0).steps().stream().map(step -> step.stepName()).toList());
        assertEquals(List.of("B"), jobs.get(1).steps().stream().map(step -> step.stepName()).toList());
    }

    @Test
    void namespacesProcedureLocalCondAndIfReferences() {
        JclJob parsed = new JclParser().parseAll(List.of(
                new org.shark.renovatio.jcl.parse.JclSource("batch/main.jcl",
                        "//MAINJOB JOB\n//CALL EXEC PROC=FLOW\n"),
                new org.shark.renovatio.jcl.parse.JclSource("batch/flow.jcl", """
                        //FLOW PROC
                        //EARLIER EXEC PGM=A
                        //LATER EXEC PGM=B,COND=(0,NE,EARLIER)
                        // IF EARLIER.RC = 0 THEN
                        //FINAL EXEC PGM=C
                        // ENDIF
                        // PEND
                        """))).stream().filter(job -> job.jobName().equals("MAINJOB")).findFirst().orElseThrow();
        assertEquals("CALL_EARLIER", parsed.steps().get(1).condition().orElseThrow()
                .predicates().get(0).referencedStep().orElseThrow());
        assertEquals("CALL_EARLIER.RC = 0", parsed.steps().get(2).ifExpression().orElseThrow());
        assertDoesNotThrow(() -> new BatchJobProjection().project(parsed, List.of(), "d".repeat(64)));
    }

    @Test
    void characterizationHonorsOnlyAfterNormalCompletion() throws Exception {
        BatchJob job = project("//ONLYJOB JOB\n//ONE EXEC PGM=A\n//TWO EXEC PGM=B,COND=ONLY\n");
        BatchCharacterizationHarness.RunResult result = new BatchCharacterizationHarness().run(job, Map.of(),
                (step, datasets) -> 0);
        assertEquals(List.of("ONE"), result.executedSteps());
    }

    @Test
    void rejectsUnsupportedSortTransformations() {
        SortUtility sort = new SortUtility();
        assertThrows(UnsupportedOperationException.class,
                () -> sort.execute(List.of("001", "001"), "SORT FIELDS=(1,3,CH,A) SUM FIELDS=NONE"));
        assertThrows(UnsupportedOperationException.class,
                () -> sort.parse("SORT FIELDS=(1,3,CH,A) OUTREC FIELDS=(1,3)"));
    }

    @Test
    void rejectsCompoundSortFilterInsteadOfPartiallyMatchingIt() {
        SortUtility sort = new SortUtility();
        assertThrows(UnsupportedOperationException.class, () -> sort.parse(
                "SORT FIELDS=(1,3,CH,A) INCLUDE COND=(1,1,CH,EQ,C'A',AND,2,1,CH,EQ,C'B')"));
        SortUtility.SortSpec simple = sort.parse("SORT FIELDS=(1,3,CH,A) INCLUDE COND=(4,1,CH,EQ,C'Y')");
        assertTrue(simple.filter().isPresent());
    }

    @Test
    void characterizationDoesNotSkipWhenAGuardReferencesABypassedStep() throws Exception {
        BatchJob job = project("""
                //CHAINJOB JOB
                //S0 EXEC PGM=P0
                // IF (S0.RC > 0) THEN
                //S1 EXEC PGM=P1
                // ENDIF
                //S2 EXEC PGM=P2,COND=(0,NE,S1)
                """);

        BatchCharacterizationHarness.RunResult result = new BatchCharacterizationHarness().run(job,
                new LinkedHashMap<>(), (step, datasets) -> 0);

        assertEquals(List.of("S0", "S2"), result.executedSteps());
    }

    @Test
    void rejectsBinaryAndPackedSortKeysAsUnsupported() {
        SortUtility sort = new SortUtility();
        assertThrows(UnsupportedOperationException.class,
                () -> sort.parse("SORT FIELDS=(1,4,BI,A)"));
        assertThrows(UnsupportedOperationException.class,
                () -> sort.parse("SORT FIELDS=(1,3,CH,A) INCLUDE COND=(5,2,PD,GT,10)"));
    }

    @Test
    void substitutesExactSymbolicNamesWhenOnePrefixesAnother() {
        JclJob job = new JclParser().parse("batch/sym.jcl",
                "//SYMJOB JOB\n// SET A=X,AB=YY\n//S EXEC PGM=&AB\n");
        assertEquals("YY", job.steps().get(0).executable());
    }

    @Test
    void keepsCataloguedOldPassDatasetFileBacked() {
        BatchJob job = project("""
                //CATJOB JOB
                //RUN EXEC PGM=IEBGENER
                //IN DD DSN=PROD.INPUT,DISP=(OLD,PASS)
                """);
        BatchDataset in = job.datasets().stream().filter(d -> d.ddName().equals("IN"))
                .findFirst().orElseThrow();
        assertNotEquals(BatchDataset.AccessKind.TEMP, in.access());
    }

    @Test
    void propagatesProcInvocationCondToExpandedSteps() {
        JclJob job = new JclParser().parseAll(List.of(
                new org.shark.renovatio.jcl.parse.JclSource("batch/m.jcl",
                        "//MJOB JOB\n//PRE EXEC PGM=PRE\n//CALL EXEC PROC=FLOW,COND=(0,NE,PRE)\n"),
                new org.shark.renovatio.jcl.parse.JclSource("batch/flow.jcl",
                        "//FLOW PROC\n//INNER EXEC PGM=INNER\n// PEND\n")))
                .stream().filter(j -> j.jobName().equals("MJOB")).findFirst().orElseThrow();
        var inner = job.steps().get(1).condition().orElseThrow();
        assertEquals("PRE", inner.predicates().get(0).referencedStep().orElseThrow());
    }

    @Test
    void nestedIfScopesKeepTheOuterGuardActive() throws Exception {
        BatchJob job = project("""
                //NESTJOB JOB
                //A EXEC PGM=A
                // IF (A.RC = 0) THEN
                //B EXEC PGM=B
                // IF (B.RC = 0) THEN
                //C EXEC PGM=C
                // ENDIF
                //D EXEC PGM=D
                // ENDIF
                //E EXEC PGM=E
                """);

        // A fails: the outer IF is false, so B, C and D are all bypassed; E always runs.
        BatchCharacterizationHarness.RunResult result = new BatchCharacterizationHarness().run(job,
                new LinkedHashMap<>(), (step, datasets) -> step.stepName().equals("A") ? 8 : 0);

        assertEquals(List.of("A", "E"), result.executedSteps());
    }

    @Test
    void classifiesUnsupportedIdcamsControlAsResidue() {
        BatchJob job = project("""
                //IDCJOB JOB
                //CAT EXEC PGM=IDCAMS
                //SYSIN DD *
                 LISTCAT ENT(TEST.DATA)
                /*
                """);
        assertEquals(org.shark.renovatio.semantic.ir.BatchStep.Kind.RESIDUE, job.steps().get(0).kind());
        assertTrue(job.steps().get(0).residueReason().orElseThrow().contains("unsupported IDCAMS"));
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
