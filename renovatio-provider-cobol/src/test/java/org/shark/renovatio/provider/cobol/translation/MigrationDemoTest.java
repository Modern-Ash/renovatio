package org.shark.renovatio.provider.cobol.translation;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.java.OpenRewriteRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates the improved COBOL to Java migration without TODO comments
 */
class MigrationDemoTest {

    private static final String COBOL_PROGRAM = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. CALC.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 WS-NUM1 PIC 9(5) VALUE 0.
            01 WS-NUM2 PIC 9(5) VALUE 0.
            01 WS-RESULT PIC 9(10) VALUE 0.
            01 WS-MESSAGE PIC X(50).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 100 TO WS-NUM1.
                MOVE 200 TO WS-NUM2.
                ADD WS-NUM1 TO WS-NUM2 GIVING WS-RESULT.
                MOVE 'Calculation completed' TO WS-MESSAGE.
            """;

    private static final String JAVA_STUB = """
            package example;
            public class CalcDTO {
                private Integer wsNum1;
                private Integer wsNum2;
                private Long wsResult;
                private String wsMessage;
                
                public Integer getWsNum1() { return wsNum1; }
                public void setWsNum1(Integer wsNum1) { this.wsNum1 = wsNum1; }
                public Integer getWsNum2() { return wsNum2; }
                public void setWsNum2(Integer wsNum2) { this.wsNum2 = wsNum2; }
                public Long getWsResult() { return wsResult; }
                public void setWsResult(Long wsResult) { this.wsResult = wsResult; }
                public String getWsMessage() { return wsMessage; }
                public void setWsMessage(String wsMessage) { this.wsMessage = wsMessage; }
            }
            
            public class CalcService {
                public CalcDTO process(CalcDTO input) {
                    CalcDTO output = new CalcDTO();
                    return output;
                }
            }
            """;

    @Test
    void demonstrateImprovedMigration() {
        CobolIntermediateModelService modelService = new CobolIntermediateModelService();
        CobolIntermediateModel model = modelService.parse(COBOL_PROGRAM);
        CobolSemanticTranspiler transpiler = new CobolSemanticTranspiler(new OpenRewriteRunner());

        String generatedJava = transpiler.enrichServiceImplementation(JAVA_STUB, model);
        
        System.out.println("=== Generated Java Code (Without TODO comments) ===");
        System.out.println(generatedJava);
        System.out.println("=================================================");
        
        // Verify no TODO comments
        assertThat(generatedJava).doesNotContain("TODO");
        assertThat(generatedJava).doesNotContain("Implement COBOL business logic");
        
        // Verify actual COBOL logic is translated (basic statements)
        assertThat(generatedJava).contains("output.setWsNum1(100);");
        assertThat(generatedJava).contains("output.setWsNum2(200);");
        assertThat(generatedJava).contains("output.setWsMessage(\"Calculation completed\");");
        
        System.out.println("✓ Migration successful: Java code generated with actual COBOL logic");
        System.out.println("✓ No TODO comments found in generated code");
    }
}
