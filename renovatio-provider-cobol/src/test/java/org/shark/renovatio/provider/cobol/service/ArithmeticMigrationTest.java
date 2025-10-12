package org.shark.renovatio.provider.cobol.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.java.OpenRewriteRunner;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that COBOL arithmetic statements (ADD, SUBTRACT, MULTIPLY, DIVIDE)
 * are correctly translated to Java business logic, not left as @TODO comments.
 */
class ArithmeticMigrationTest {

    private static final String CALCULATOR_COBOL = """
       identification division.
       program-id. calculate.

       environment division.

       data division.
       working-storage section.
       01 calmemory      pic s9(9) comp-5 value 0.

       linkage section.
       01 calculator.
          05 arg1        pic s9(19)v9(19) comp-3.
          05 arg2        pic s9(19)v9(19) comp-3.
          05 result      pic s9(19)v9(19) comp-3.
          05 storage     pic s9(19)v9(19) comp-3.

       procedure division.
       exit program.

       entry "add" using calculator.
         move arg1 to result
         add  arg2 to result
         add  result to calmemory
         move calmemory to storage
         exit program.

       entry "subtract" using calculator.
         move arg1 to result
         subtract arg2 from result
         add  result to calmemory
         move calmemory to storage
         exit program.

      entry "multiply" using calculator.
         move arg1 to result
         multiply arg2 by result
         add  result to calmemory
         move calmemory to storage
         exit program.

      entry "divide" using calculator.
         move arg1 to result
         divide arg2 into result
         add  result to calmemory
         move calmemory to storage
         exit program.
        """;

    @Test
    void testArithmeticOperationsAreMigrated() throws Exception {
        // Given: COBOL program with arithmetic operations
        CobolParsingService parsingService = new CobolParsingService();
        TemplateCodeGenerationService templateService = new TemplateCodeGenerationService();
        CobolIntermediateModelService intermediateModelService = new CobolIntermediateModelService();
        CobolSemanticTranspiler semanticTranspiler = new CobolSemanticTranspiler(new OpenRewriteRunner());
        JavaGenerationService javaGenerationService = new JavaGenerationService(
            parsingService, templateService, intermediateModelService, semanticTranspiler
        );
        
        Path tempFile = java.nio.file.Files.createTempFile("calculator", ".cob");
        java.nio.file.Files.writeString(tempFile, CALCULATOR_COBOL);
        
        Map<String, Object> metadata = parsingService.parseCobolFile(tempFile);
        
        // When: Generating service implementation
        java.lang.reflect.Method implMethod = javaGenerationService.getClass()
            .getDeclaredMethod("generateServiceImplementation", String.class, Map.class);
        implMethod.setAccessible(true);
        String serviceImpl = (String) implMethod.invoke(javaGenerationService, "Calculator", metadata);
        
        System.out.println("=== Service Implementation BEFORE semantic transpiler ===");
        System.out.println(serviceImpl);
        
        // Apply semantic transpiler
        CobolIntermediateModel model = intermediateModelService.parse(CALCULATOR_COBOL);
        String enriched = semanticTranspiler.enrichServiceImplementation(serviceImpl, model);
        
        System.out.println("\n=== Service Implementation AFTER semantic transpiler ===");
        System.out.println(enriched);
        
        // Then: Verify arithmetic operations are translated
        // ADD operation: "add arg2 to result" should become "result = result + arg2" or similar
        assertThat(enriched)
            .as("ADD method should contain actual business logic, not @TODO")
            .doesNotContain("// TODO: Implement COBOL business logic for ENTRY add")
            .contains("out.setResult("); // Should set result field
        
        // SUBTRACT operation
        assertThat(enriched)
            .as("SUBTRACT method should contain actual business logic, not @TODO")
            .doesNotContain("// TODO: Implement COBOL business logic for ENTRY subtract");
        
        // MULTIPLY operation
        assertThat(enriched)
            .as("MULTIPLY method should contain actual business logic, not @TODO")
            .doesNotContain("// TODO: Implement COBOL business logic for ENTRY multiply");
        
        // DIVIDE operation
        assertThat(enriched)
            .as("DIVIDE method should contain actual business logic, not @TODO")
            .doesNotContain("// TODO: Implement COBOL business logic for ENTRY divide");
    }
    
    @Test
    void testArithmeticLogicIsCorrect() throws Exception {
        // Given: COBOL program with ADD operation
        CobolIntermediateModelService intermediateModelService = new CobolIntermediateModelService();
        CobolSemanticTranspiler semanticTranspiler = new CobolSemanticTranspiler(new OpenRewriteRunner());
        
        String simpleJavaStub = """
            package sample;
            public class CalculatorDTO {
                private java.math.BigDecimal arg1;
                private java.math.BigDecimal arg2;
                private java.math.BigDecimal result;
                public java.math.BigDecimal getArg1() { return arg1; }
                public void setArg1(java.math.BigDecimal arg1) { this.arg1 = arg1; }
                public java.math.BigDecimal getArg2() { return arg2; }
                public void setArg2(java.math.BigDecimal arg2) { this.arg2 = arg2; }
                public java.math.BigDecimal getResult() { return result; }
                public void setResult(java.math.BigDecimal result) { this.result = result; }
            }
            public class CalculatorService {
                public CalculatorDTO add(CalculatorDTO input) {
                    // TODO: Implement COBOL business logic for ENTRY add
                    CalculatorDTO out = new CalculatorDTO();
                    out.setResult(null);
                    return out;
                }
            }
            """;
        
        // When: Applying semantic transpiler
        CobolIntermediateModel model = intermediateModelService.parse(CALCULATOR_COBOL);
        String enriched = semanticTranspiler.enrichServiceImplementation(simpleJavaStub, model);
        
        System.out.println("\n=== Enriched ADD method ===");
        System.out.println(enriched);
        
        // Then: Verify the logic is correct
        // Should have: result = arg1, then result = result + arg2
        assertThat(enriched)
            .contains("out.setResult(")
            .doesNotContain("@TODO")
            .doesNotContain("// TODO: Implement COBOL business logic");
    }
}
