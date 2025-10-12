package org.shark.renovatio.provider.cobol.translation;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.java.OpenRewriteRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NoTodoCommentsTest {

    private static final String COBOL_WITH_STATEMENTS = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. TESTPROG.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 WS-COUNTER PIC 9(3) VALUE 0.
            01 WS-NAME PIC X(20).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'JOHN DOE' TO WS-NAME.
                ADD 1 TO WS-COUNTER.
            """;

    private static final String COBOL_EMPTY_PROCEDURE = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. EMPTYPROG.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 WS-DATA PIC X(10).
            PROCEDURE DIVISION.
            MAIN-PARA.
            """;

    private static final String JAVA_STUB_WITH_STATEMENTS = """
            package test;
            public class TestprogDTO {
                private String wsName;
                private Integer wsCounter;
                public String getWsName() { return wsName; }
                public void setWsName(String wsName) { this.wsName = wsName; }
                public Integer getWsCounter() { return wsCounter; }
                public void setWsCounter(Integer wsCounter) { this.wsCounter = wsCounter; }
            }
            public class TestprogService {
                public TestprogDTO process(TestprogDTO input) {
                    TestprogDTO output = new TestprogDTO();
                    return output;
                }
            }
            """;

    private static final String JAVA_STUB_EMPTY = """
            package test;
            public class EmptyprogDTO {
                private String wsData;
                public String getWsData() { return wsData; }
                public void setWsData(String wsData) { this.wsData = wsData; }
            }
            public class EmptyprogService {
                public EmptyprogDTO process(EmptyprogDTO input) {
                    EmptyprogDTO output = new EmptyprogDTO();
                    return output;
                }
            }
            """;

    @Test
    void shouldNotContainTodoCommentsWhenStatementsExist() {
        CobolIntermediateModelService modelService = new CobolIntermediateModelService();
        CobolIntermediateModel model = modelService.parse(COBOL_WITH_STATEMENTS);
        CobolSemanticTranspiler transpiler = new CobolSemanticTranspiler(new OpenRewriteRunner());

        String enriched = transpiler.enrichServiceImplementation(JAVA_STUB_WITH_STATEMENTS, model);
        
        assertThat(enriched).doesNotContain("TODO");
        assertThat(enriched).doesNotContain("Implement COBOL business logic");
        assertThat(enriched).contains("output.setWsName(\"JOHN DOE\");");
        assertThat(enriched).contains("output.setWsCounter(");
    }

    @Test
    void shouldNotContainTodoCommentsEvenWhenProcedureIsEmpty() {
        CobolIntermediateModelService modelService = new CobolIntermediateModelService();
        CobolIntermediateModel model = modelService.parse(COBOL_EMPTY_PROCEDURE);
        CobolSemanticTranspiler transpiler = new CobolSemanticTranspiler(new OpenRewriteRunner());

        String enriched = transpiler.enrichServiceImplementation(JAVA_STUB_EMPTY, model);
        
        assertThat(enriched).doesNotContain("TODO");
        assertThat(enriched).doesNotContain("Implement COBOL business logic");
        // Should still have valid method structure
        assertThat(enriched).contains("public EmptyprogDTO process(EmptyprogDTO input)");
        assertThat(enriched).contains("return output;");
    }
}
