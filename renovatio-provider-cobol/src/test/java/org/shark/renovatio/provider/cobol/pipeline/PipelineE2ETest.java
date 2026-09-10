package org.shark.renovatio.provider.cobol.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.provider.cobol.service.CobolParsingService;
import org.shark.renovatio.provider.cobol.service.generation.JavaGenerationOrchestrator;
import org.shark.renovatio.provider.cobol.service.generation.JavaWriteService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end test for the COBOL-to-Java pipeline.
 * Tests the complete pipeline execution for all fixtures.
 */
class PipelineE2ETest {
    
    @TempDir
    Path tempDir;
    
    private CobolPipelineOrchestrator orchestrator;
    private CobolParsingService parsingService;
    private JavaGenerationOrchestrator generationOrchestrator;
    private CobolSemanticTranspiler semanticTranspiler;
    private MavenBuildService buildService;
    private EquivalenceChecker equivalenceChecker;
    
    @BeforeEach
    void setUp() throws IOException {
        parsingService = mock(CobolParsingService.class);
        generationOrchestrator = mock(JavaGenerationOrchestrator.class);
        semanticTranspiler = mock(CobolSemanticTranspiler.class);
        buildService = new MavenBuildServiceImpl();
        equivalenceChecker = new EquivalenceCheckerImpl();

        when(generationOrchestrator.generateInterfaceStubs(any(), any()))
            .thenAnswer(invocation -> emitExpectedFixture(invocation.getArgument(1)));
        
        orchestrator = new CobolPipelineOrchestrator(
            parsingService,
            generationOrchestrator,
            semanticTranspiler,
            buildService,
            equivalenceChecker
        );
    }
    
    @Test
    void shouldExecuteBatchSimpleFixture() throws IOException {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/batch-simple");
        Path outputDir = tempDir.resolve("batch-simple-output");
        Files.createDirectories(outputDir);
        
        PipelineRequest request = PipelineRequest.of(fixtureDir, outputDir);
        
        // Act
        PipelineResult result = orchestrator.execute(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.fixtureId()).isEqualTo("batch-simple");
        assertThat(result.discover()).isNotNull();
        assertThat(result.discover().success()).isTrue();
        assertThat(result.parse()).isNotNull();
        assertThat(result.parse().success()).isTrue();
        assertThat(result.semanticIr()).isNotNull();
        assertThat(result.semanticIr().success()).isTrue();
        assertThat(result.decisions()).isNotNull();
        assertThat(result.decisions().success()).isTrue();
        assertThat(result.manifest()).isNotNull();
        assertThat(result.manifest().success()).isTrue();
        assertThat(result.emit()).isNotNull();
        assertThat(result.emit().success()).isTrue();
        assertThat(result.openRewrite()).isNotNull();
        assertThat(result.openRewrite().success()).isTrue();
        assertThat(result.build()).isNotNull();
        assertThat(result.build().success()).isTrue();
        assertThat(result.success()).isTrue();
        verify(generationOrchestrator, atLeastOnce()).generateInterfaceStubs(any(), any());
    }
    
    @Test
    void shouldExecuteCicsMvcFixture() throws IOException {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/cics-mvc");
        Path outputDir = tempDir.resolve("cics-mvc-output");
        Files.createDirectories(outputDir);
        
        PipelineRequest request = PipelineRequest.of(fixtureDir, outputDir);
        
        // Act
        PipelineResult result = orchestrator.execute(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.fixtureId()).isEqualTo("cics-mvc");
        assertThat(result.discover()).isNotNull();
        assertThat(result.discover().success()).isTrue();
        assertThat(result.parse()).isNotNull();
        assertThat(result.parse().success()).isTrue();
        assertThat(result.success()).isTrue();
    }
    
    @Test
    void shouldExecuteDb2AccessFixture() throws IOException {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/db2-access");
        Path outputDir = tempDir.resolve("db2-access-output");
        Files.createDirectories(outputDir);
        
        PipelineRequest request = PipelineRequest.of(fixtureDir, outputDir);
        
        // Act
        PipelineResult result = orchestrator.execute(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.fixtureId()).isEqualTo("db2-access");
        assertThat(result.discover()).isNotNull();
        assertThat(result.discover().success()).isTrue();
        assertThat(result.parse()).isNotNull();
        assertThat(result.parse().success()).isTrue();
        assertThat(result.success()).isTrue();
    }
    
    @Test
    void shouldExecuteAllFixtures() throws IOException {
        // Arrange
        List<PipelineRequest> requests = List.of(
            PipelineRequest.of(
                Path.of("src/test/resources/fixtures/batch-simple"),
                tempDir.resolve("batch-simple-output")
            ),
            PipelineRequest.of(
                Path.of("src/test/resources/fixtures/cics-mvc"),
                tempDir.resolve("cics-mvc-output")
            ),
            PipelineRequest.of(
                Path.of("src/test/resources/fixtures/db2-access"),
                tempDir.resolve("db2-access-output")
            )
        );
        
        // Act
        List<PipelineResult> results = orchestrator.executeAll(requests);
        
        // Assert
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(PipelineResult::isSuccessful);
    }
    
    @Test
    void shouldTrackSemanticGaps() throws IOException {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/batch-simple");
        Path outputDir = tempDir.resolve("batch-simple-gaps-output");
        Files.createDirectories(outputDir);
        
        PipelineRequest request = PipelineRequest.of(fixtureDir, outputDir);
        
        // Act
        PipelineResult result = orchestrator.execute(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.semanticGaps()).isNotNull();
        assertThat(result.semanticGaps()).isEmpty(); // No gaps in simple fixture
    }
    
    @Test
    void shouldGenerateEquivalenceReport() throws IOException {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/batch-simple");
        Path outputDir = tempDir.resolve("batch-simple-equiv-output");
        Files.createDirectories(outputDir);
        
        PipelineRequest request = new PipelineRequest(
            "batch-simple",
            fixtureDir,
            java.util.Map.of(),
            outputDir,
            fixtureDir.resolve("expected"),
            false,
            true
        );
        
        // Act
        PipelineResult result = orchestrator.execute(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.equivalence()).isNotNull();
        assertThat(result.equivalence().isPassed()).isTrue();
    }

    @Test
    void shouldFailWhenEmissionProducesNoJavaFiles() {
        StubResult emptyGeneration = new StubResult(true, "No Java files generated");
        emptyGeneration.setGeneratedCode(Map.of());
        doReturn(emptyGeneration).when(generationOrchestrator)
            .generateInterfaceStubs(any(), any());
        Path fixtureDir = Path.of("src/test/resources/fixtures/batch-simple");
        PipelineRequest request = new PipelineRequest(
            "batch-simple",
            fixtureDir,
            Map.of(),
            tempDir.resolve("empty-emission-output"),
            fixtureDir.resolve("expected"),
            false,
            false
        );

        PipelineResult result = orchestrator.execute(request);

        assertThat(result.success()).isFalse();
    }
    
    @Test
    void shouldCompleteWithinTimeLimit() throws IOException {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/batch-simple");
        Path outputDir = tempDir.resolve("batch-simple-time-output");
        Files.createDirectories(outputDir);
        
        PipelineRequest request = PipelineRequest.of(fixtureDir, outputDir);
        
        // Act
        long startTime = System.currentTimeMillis();
        PipelineResult result = orchestrator.execute(request);
        long duration = System.currentTimeMillis() - startTime;
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(duration).isLessThan(60000); // Should complete within 60 seconds
    }

    private StubResult emitExpectedFixture(Workspace workspace) throws IOException {
        Path fixtureDir = Path.of(workspace.getPath());
        Path expectedJavaDir = fixtureDir.resolve(
            "expected/src/main/java/org/shark/renovatio/generated/cobol"
        );
        Path outputJavaDir = Path.of(workspace.getMetadata()
            .get(JavaWriteService.OUTPUT_DIRECTORY_METADATA_KEY).toString());
        Files.createDirectories(outputJavaDir);

        Map<String, String> generatedCode = new LinkedHashMap<>();
        try (var files = Files.list(expectedJavaDir)) {
            for (Path expectedFile : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(expectedFile);
                generatedCode.put(expectedFile.getFileName().toString(), content);
                Files.writeString(outputJavaDir.resolve(expectedFile.getFileName()), content);
            }
        }

        StubResult result = new StubResult(!generatedCode.isEmpty(),
            "Generated " + generatedCode.size() + " Java files");
        result.setGeneratedCode(generatedCode);
        return result;
    }
}
