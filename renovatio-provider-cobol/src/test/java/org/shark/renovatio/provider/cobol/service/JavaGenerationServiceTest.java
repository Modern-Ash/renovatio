package org.shark.renovatio.provider.cobol.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Java generation service
 */
class JavaGenerationServiceTest {

    private Path tempDir;
    private JavaGenerationService javaGenerationService;
    private Workspace workspace;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("cobol-ws-");
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        TemplateCodeGenerationService tmpl = new TemplateCodeGenerationService();
        javaGenerationService = new JavaGenerationService(parsing, tmpl, new org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService(), new org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler(new org.shark.renovatio.provider.java.OpenRewriteRunner()));
        workspace = new Workspace("test", tempDir.toString(), "main");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null) {
            // best-effort cleanup
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }
    }

    @Test
    void testGenerateInterfaceStubsWithSampleCobol() throws IOException {
        // Create a sample COBOL file
        String cobolContent = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE-PROGRAM.
                
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01  WS-NAME       PIC X(30).
                01  WS-AGE        PIC 9(3).
                01  WS-SALARY     PIC 9(8)V99.
                
                PROCEDURE DIVISION.
                MAIN-PARA.
                    DISPLAY "Hello World".
                    STOP RUN.
                """;

        Path cobolFile = tempDir.resolve("sample.cob");
        Files.writeString(cobolFile, cobolContent);

        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("stubs");
        query.setLanguage("cobol");

        var result = javaGenerationService.generateInterfaceStubs(query, workspace);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getGeneratedCode());
        assertFalse(result.getGeneratedCode().isEmpty());

        // Check that Java files were generated
        assertTrue(result.getGeneratedCode().containsKey("SampleProgramDTO.java") ||
                   result.getGeneratedCode().containsKey("SampleDTO.java"));
        assertTrue(result.getGeneratedCode().containsKey("SampleProgramService.java") ||
                   result.getGeneratedCode().containsKey("SampleService.java"));
        assertTrue(result.getGeneratedCode().containsKey("SampleProgramServiceImpl.java") ||
                   result.getGeneratedCode().containsKey("SampleServiceImpl.java"));

        // Verify DTO contains expected fields
        String dtoKey = result.getGeneratedCode().containsKey("SampleProgramDTO.java") ? "SampleProgramDTO.java" : "SampleDTO.java";
        String dtoCode = result.getGeneratedCode().get(dtoKey);
        assertNotNull(dtoCode);
        assertTrue(dtoCode.contains("class " + dtoKey.replace(".java", "")));
        assertTrue(dtoCode.contains("String wsName"));
        assertTrue(dtoCode.contains("Integer wsAge"));
        assertTrue(dtoCode.contains("BigDecimal wsSalary"));
    }

    @Test
    void testGenerateInterfaceStubsWithEmptyWorkspace() {
        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("stubs");

        StubResult result = javaGenerationService.generateInterfaceStubs(query, workspace);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getGeneratedCode());
        // Should be empty but not null for empty workspace
        assertTrue(result.getGeneratedCode().isEmpty());
    }

    @Test
    void generatesEntryMethodsAndDtoFromLinkage() throws Exception {
        String cobol = """
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

        Path file = tempDir.resolve("calculate.cob");
        Files.writeString(file, cobol);

        NqlQuery q = new NqlQuery();

        var result = javaGenerationService.generateInterfaceStubs(q, workspace);
        assertTrue(result.isSuccess(), "generation should succeed: " + result.getMessage());
        Map<String, String> files = result.getGeneratedCode();
        assertNotNull(files);

        // Class base is derived from program-id: Calculate
        assertTrue(files.containsKey("CalculateDTO.java"));
        assertTrue(files.containsKey("CalculateService.java"));
        assertTrue(files.containsKey("CalculateServiceImpl.java"));

        String dto = files.get("CalculateDTO.java");
        assertAll(
                () -> assertTrue(dto.contains("private BigDecimal arg1")),
                () -> assertTrue(dto.contains("private BigDecimal arg2")),
                () -> assertTrue(dto.contains("private BigDecimal result")),
                () -> assertTrue(dto.contains("private BigDecimal storage"))
        );

        String iface = files.get("CalculateService.java");
        assertAll(
                () -> assertTrue(iface.contains("process(")),
                () -> assertTrue(iface.contains("validate(")),
                () -> assertTrue(iface.contains("add(")),
                () -> assertTrue(iface.contains("subtract(")),
                () -> assertTrue(iface.contains("multiply(")),
                () -> assertTrue(iface.contains("divide("))
        );

        String impl = files.get("CalculateServiceImpl.java");
        assertAll(
                () -> assertTrue(impl.contains("class CalculateServiceImpl")),
                () -> assertTrue(impl.contains("@Service")),
                () -> assertTrue(impl.contains("out.setResult"))
        );

        // Also verify files written to disk exist
        Path genDir = tempDir.resolve("generated-java-stubs");
        assertTrue(Files.exists(genDir.resolve("CalculateDTO.java")));
        assertTrue(Files.exists(genDir.resolve("CalculateService.java")));
        assertTrue(Files.exists(genDir.resolve("CalculateServiceImpl.java")));
    }
}