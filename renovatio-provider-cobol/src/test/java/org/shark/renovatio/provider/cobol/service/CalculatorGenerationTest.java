package org.shark.renovatio.provider.cobol.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.java.OpenRewriteRunner;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for calculator COBOL program with ENTRY statements
 */
class CalculatorGenerationTest {

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
    void testParseCalculatorEntries() throws Exception {
        CobolParsingService parsingService = new CobolParsingService();
        Path tempFile = java.nio.file.Files.createTempFile("calculator", ".cob");
        java.nio.file.Files.writeString(tempFile, CALCULATOR_COBOL);
        
        Map<String, Object> metadata = parsingService.parseCobolFile(tempFile);
        
        // Check that entries are extracted
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> entries = (java.util.List<Map<String, Object>>) metadata.get("entries");

        assertThat(entries)
                .isNotNull()
                .hasSize(4)
                .extracting(e -> e.get("name"))
                .containsExactlyInAnyOrder("add", "subtract", "multiply", "divide");

        // Check that linkage items are extracted
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> linkageItems = (java.util.List<Map<String, Object>>) metadata.get("linkageItems");

        assertThat(linkageItems)
                .isNotNull()
                .hasSize(4)
                .extracting(item -> item.get("name"))
                .containsExactlyInAnyOrder("arg1", "arg2", "result", "storage");
    }

    @Test
    void testIRParserExtractsEntryParagraphs() {
        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(CALCULATOR_COBOL);
        
        // Check that ENTRY paragraphs are extracted
        assertThat(model.findParagraph("ADD")).isPresent();
        assertThat(model.findParagraph("SUBTRACT")).isPresent();
        assertThat(model.findParagraph("MULTIPLY")).isPresent();
        assertThat(model.findParagraph("DIVIDE")).isPresent();

        // Check that each paragraph has statements
        for (String name : List.of("ADD", "SUBTRACT", "MULTIPLY", "DIVIDE")) {
            var paragraph = model.findParagraph(name).orElseThrow();
            assertThat(paragraph.statements()).isNotEmpty();
        }
    }

    @Test
    void testJavaGenerationCreatesMethodsForEntries() throws Exception {
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
        
        // Use reflection to call private methods
        java.lang.reflect.Method dtoMethod = javaGenerationService.getClass()
            .getDeclaredMethod("generateDataTransferObject", String.class, Map.class);
        dtoMethod.setAccessible(true);
        String dtoCode = (String) dtoMethod.invoke(javaGenerationService, "Calculator", metadata);
        
        System.out.println("Generated DTO:\n" + dtoCode);
        
        // Check that DTO has fields from linkage section
        assertThat(dtoCode)
                .contains(
                        "private BigDecimal arg1",
                        "private BigDecimal arg2",
                        "private BigDecimal result",
                        "private BigDecimal storage"
                );

        // Generate Service Interface
        java.lang.reflect.Method interfaceMethod = javaGenerationService.getClass()
            .getDeclaredMethod("generateServiceInterface", String.class, Map.class);
        interfaceMethod.setAccessible(true);
        String serviceInterface = (String) interfaceMethod.invoke(javaGenerationService, "Calculator", metadata);
        
        System.out.println("Generated Service Interface:\n" + serviceInterface);
        
        // Check that service interface has methods for each ENTRY
        assertThat(serviceInterface)
                .contains(
                        "CalculatorDTO add(CalculatorDTO input)",
                        "CalculatorDTO subtract(CalculatorDTO input)",
                        "CalculatorDTO multiply(CalculatorDTO input)",
                        "CalculatorDTO divide(CalculatorDTO input)"
                );
    }
}
