package org.shark.renovatio.provider.cobol.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.provider.cobol.service.CobolParsingService;
import org.shark.renovatio.provider.cobol.service.generation.JavaGenerationOrchestrator;
import org.shark.renovatio.provider.cobol.service.generation.JavaWriteService;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for the COBOL-to-Java pipeline.
 * Tests the complete pipeline execution for all fixtures.
 */
class PipelineE2ETest {
    
    @TempDir
    Path tempDir;
    
    private CobolPipelineOrchestrator orchestrator;
    private CobolParsingService parsingService;
    private FixtureGenerationOrchestrator generationOrchestrator;
    private CobolSemanticTranspiler semanticTranspiler;
    private MavenBuildService buildService;
    private EquivalenceChecker equivalenceChecker;
    
    @BeforeEach
    void setUp() throws IOException {
        parsingService = new CobolParsingService();
        generationOrchestrator = new FixtureGenerationOrchestrator();
        semanticTranspiler = new CobolSemanticTranspiler();
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
        assertThat(result.domainModel()).isNotNull();
        assertThat(result.domainModel().success()).isTrue();
        assertThat(result.decisions()).isNotNull();
        assertThat(result.decisions().success()).isTrue();
        assertThat(result.architecture()).isNotNull();
        assertThat(result.architecture().success()).isTrue();
        assertThat(result.manifest()).isNotNull();
        assertThat(result.manifest().success()).isTrue();
        assertThat(result.emit()).isNotNull();
        assertThat(result.emit().success()).isTrue();
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
    void shouldExecuteProductionPipelineForAllFixtures() {
        CobolPipelineOrchestrator production = productionPipeline();

        for (String fixture : List.of("batch-simple", "cics-mvc", "db2-access")) {
            Path fixtureDir = Path.of("src/test/resources/fixtures").resolve(fixture);
            Path outputDir = tempDir.resolve("production-" + fixture);
            PipelineResult result = production.execute(new PipelineRequest(
                fixture, fixtureDir, PipelineRequest.of(fixtureDir, outputDir).decisions(),
                outputDir, fixtureDir.resolve("expected"), false, true));

            assertThat(result.isSuccessful())
                .as("production pipeline %s: %s", fixture, result)
                .isTrue();
            assertThat(result.equivalenceReports())
                .as("complete golden-file equivalence for %s", fixture)
                .isNotEmpty()
                .allMatch(EquivalenceReport::isPassed);
            assertThat(result.semanticGaps()).noneMatch(ActionItem::isBlocking);
        }
    }

    @Test
    void shouldValidateProductionDeterminismAndIdempotency() {
        Path fixtureDir = Path.of("src/test/resources/fixtures/batch-simple");
        PipelineRequest request = PipelineRequest.of(fixtureDir,
            tempDir.resolve("production-repeatability"));
        CobolPipelineOrchestrator production = productionPipeline();

        var deterministic = new DeterminismValidator().validate(production, request, 2);
        assertThat(deterministic.isDeterministic()).as("determinism: %s", deterministic.divergences())
            .isTrue();
        var idempotent = new IdempotencyValidator().validate(production, request);
        assertThat(idempotent.isIdempotent()).as("idempotency: %s", idempotent.divergences())
            .isTrue();
    }

    private CobolPipelineOrchestrator productionPipeline() {
        CobolParsingService productionParsing = new CobolParsingService();
        JavaGenerationService productionGeneration = new JavaGenerationService(
            productionParsing,
            new org.shark.renovatio.provider.cobol.service.TemplateCodeGenerationService(),
            new org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService(),
            new CobolSemanticTranspiler());
        return new CobolPipelineOrchestrator(
            productionParsing,
            new JavaGenerationOrchestrator(productionGeneration),
            new CobolSemanticTranspiler(),
            new MavenBuildServiceImpl(),
            new EquivalenceCheckerImpl());
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
        assertThat(result.semanticGaps()).noneMatch(ActionItem::isBlocking);
        assertThat(result.semanticGaps()).anyMatch(item ->
            item.statementType().equals("UNTRANSLATED_GENERATED_CODE"));
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
        generationOrchestrator.emitEmpty = true;
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

    @Test
    void shouldRejectSourceChangedAfterRequestCreation() throws IOException {
        Path fixture = createMinimalFixture("STALE01", "MOVE KNOWN-VALUE TO KNOWN-VALUE.");
        PipelineRequest request = new PipelineRequest("stale", fixture, Map.of(),
            tempDir.resolve("stale-output"), null, false, false);

        assertThat(orchestrator.execute(request).isSuccessful()).isTrue();
        Files.writeString(fixture.resolve("src/cobol/STALE01.cbl"), "\n* changed\n",
            StandardOpenOption.APPEND);

        PipelineResult stale = orchestrator.execute(request);
        assertThat(stale.success()).isFalse();
        assertThat(stale.semanticGaps()).anyMatch(item -> item.isBlocking()
            && item.statementType().equals("STALE_SOURCE"));
    }

    @Test
    void shouldBlockAnUnclassifiedSemanticAccess() throws IOException {
        Path fixture = createMinimalFixture("GAP001", """
            MAIN-LOGIC.
                MOVE UNKNOWN-VALUE TO KNOWN-VALUE
                STOP RUN.
            """);
        PipelineResult result = orchestrator.execute(new PipelineRequest("semantic-gap", fixture,
            Map.of(), tempDir.resolve("semantic-gap-output"), null, false, false));

        assertThat(result.success()).isFalse();
        assertThat(result.semanticGaps()).anyMatch(item -> item.isBlocking()
            && item.statementType().equals("UNCLASSIFIED_DATA_ACCESS"));
    }

    private Path createMinimalFixture(String programId, String statement) throws IOException {
        Path fixture = tempDir.resolve(programId.toLowerCase());
        Path sourceDir = fixture.resolve("src/cobol");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve(programId + ".cbl"), """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. %s.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 KNOWN-VALUE PIC X(10).
            PROCEDURE DIVISION.
            %s
            """.formatted(programId, statement));
        return fixture;
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
        if (Files.isDirectory(expectedJavaDir)) {
            try (var files = Files.list(expectedJavaDir)) {
                for (Path expectedFile : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String content = Files.readString(expectedFile);
                    generatedCode.put(expectedFile.getFileName().toString(), content);
                    Files.writeString(outputJavaDir.resolve(expectedFile.getFileName()), content);
                }
            }
        } else {
            String content = "package org.shark.renovatio.generated.cobol; public class Generated {}\n";
            generatedCode.put("Generated.java", content);
            Files.writeString(outputJavaDir.resolve("Generated.java"), content);
        }

        StubResult result = new StubResult(!generatedCode.isEmpty(),
            "Generated " + generatedCode.size() + " Java files");
        result.setGeneratedCode(generatedCode);
        return result;
    }

    private final class FixtureGenerationOrchestrator extends JavaGenerationOrchestrator {
        private boolean emitEmpty;

        private FixtureGenerationOrchestrator() {
            super((JavaGenerationService) null);
        }

        @Override
        public StubResult generateInterfaceStubs(org.shark.renovatio.shared.nql.NqlQuery query,
                                                 Workspace workspace,
                                                 MigrationProfiles.EffectiveProfile effective,
                                                 String expectedManifestHash) {
            if (emitEmpty) {
                StubResult result = new StubResult(true, "No Java files generated");
                result.setGeneratedCode(Map.of());
                return result;
            }
            try {
                return emitExpectedFixture(workspace);
            } catch (IOException exception) {
                return new StubResult(false, exception.getMessage());
            }
        }
    }
}
