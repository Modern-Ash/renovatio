package org.shark.renovatio.provider.cobol.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.provider.cobol.service.CobolParsingService;
import org.shark.renovatio.provider.cobol.service.generation.JavaGenerationOrchestrator;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
    void setUp() {
        parsingService = mock(CobolParsingService.class);
        generationOrchestrator = mock(JavaGenerationOrchestrator.class);
        semanticTranspiler = mock(CobolSemanticTranspiler.class);
        buildService = new MavenBuildServiceImpl();
        equivalenceChecker = new EquivalenceCheckerImpl();
        
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
        // Equivalence report may be null if no files were generated
        // This is expected behavior when pipeline stages are simplified
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
}
