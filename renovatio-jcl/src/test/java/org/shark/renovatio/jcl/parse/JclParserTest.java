package org.shark.renovatio.jcl.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JclParserTest {
    private static final String JCL = """
            //PAYJOB JOB CLASS=A
            // SET ENV=TEST
            //LOAD EXEC PGM=LOADER
            //INPUT DD DSN=PAY.&ENV..INPUT,DISP=SHR
            //WORK DD DSN=&&TMP,DISP=(NEW,PASS)
            //ORDER EXEC PGM=SORT,COND=(0,NE,LOAD)
            //SYSIN DD *
             SORT FIELDS=(1,3,CH,A)
             INCLUDE COND=(4,1,CH,EQ,C'Y')
            /*
            //REPORT EXEC PROC=MISSING
            """;

    @Test
    void parsesThreeStepsConditionsDatasetsSymbolsAndMissingProc() {
        JclJob job = new JclParser().parse("batch/pay.jcl", JCL);
        assertEquals("PAYJOB", job.jobName());
        assertEquals(3, job.steps().size());
        assertEquals("PAY.TEST.INPUT", job.steps().get(0).ddStatements().get(0).dsn().orElseThrow());
        assertTrue(job.steps().get(0).ddStatements().get(1).temporary());
        assertEquals("LOAD.RC NE 0", job.steps().get(1).condition().orElseThrow().normalizedExpression());
        assertEquals(2, job.steps().get(1).ddStatements().get(0).instreamData().size());
        assertEquals("MISSING", job.unresolvedProcs().get(0).procName());
    }

    @Test
    void expandsCataloguedProcWithExecOverridePrecedence() {
        JclSource proc = new JclSource("procs/copy.prc", """
                //COPY PROC PGM=IEBGENER,DSN=DEFAULT.DATA
                //RUN EXEC PGM=&PGM
                //INPUT DD DSN=&DSN,DISP=SHR
                // PEND
                """);
        JclSource job = new JclSource("batch/call.jcl", """
                //CALLJOB JOB
                // SET DSN=FROM.SET
                //COPYIT EXEC PROC=COPY,PGM=SORT,DSN=FROM.EXEC
                """);
        JclJob parsed = new JclParser().parseAll(java.util.List.of(job, proc)).get(0);
        assertEquals(1, parsed.steps().size());
        assertEquals("COPYIT_RUN", parsed.steps().get(0).stepName());
        assertEquals("SORT", parsed.steps().get(0).executable());
        assertEquals("FROM.EXEC", parsed.steps().get(0).ddStatements().get(0).dsn().orElseThrow());
        assertTrue(parsed.unresolvedProcs().isEmpty());
    }
}
