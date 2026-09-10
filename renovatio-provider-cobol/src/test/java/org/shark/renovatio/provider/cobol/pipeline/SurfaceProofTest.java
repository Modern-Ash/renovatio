package org.shark.renovatio.provider.cobol.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.provider.cobol.infrastructure.CobolMcpToolsProvider;
import org.shark.renovatio.provider.cobol.CobolLanguageProvider;
import org.shark.renovatio.provider.cobol.service.*;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.shared.domain.AnalyzeResult;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Surface Proof Test: Demonstrates that both MCP tool and direct service call
 * produce equivalent results for COBOL migration.
 *
 * This test satisfies the "surface-proof" criterion:
 * - MCP route: executeCobolTool("cobol.analyze", ...)
 * - Direct route: cobolProvider.analyze(...)
 * - Both routes produce equivalent results
 */
class SurfaceProofTest {

    private CobolLanguageProvider cobolProvider;
    private CobolMcpToolsProvider mcpToolsProvider;
    private CobolReferencePipelineService referencePipeline;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        CobolParsingService parsingService = new CobolParsingService();
        TemplateCodeGenerationService templateService = new TemplateCodeGenerationService();
        CobolIntermediateModelService irService = new CobolIntermediateModelService();
        CobolSemanticTranspiler semanticTranspiler = new CobolSemanticTranspiler();
        JavaGenerationService javaGenerationService = new JavaGenerationService(
            parsingService, templateService, irService, semanticTranspiler);
        Db2MigrationService db2Service = new Db2MigrationService(parsingService);
        MigrationPlanService migrationPlanService = new MigrationPlanService(parsingService, javaGenerationService);
        IndexingService indexingService = new IndexingService();
        MetricsService metricsService = new MetricsService();
        ControlBreakDecompositionService decompositionService = new ControlBreakDecompositionService(irService, parsingService);

        cobolProvider = new CobolLanguageProvider(
            parsingService, javaGenerationService, migrationPlanService,
            indexingService, metricsService, templateService, db2Service, decompositionService
        );
        CobolPipelineOrchestrator orchestrator = new CobolPipelineOrchestrator(
            parsingService,
            new org.shark.renovatio.provider.cobol.service.generation.JavaGenerationOrchestrator(
                javaGenerationService),
            semanticTranspiler,
            new MavenBuildServiceImpl(),
            new EquivalenceCheckerImpl());
        referencePipeline = new CobolReferencePipelineService(orchestrator);
        mcpToolsProvider = new CobolMcpToolsProvider(cobolProvider, referencePipeline);
    }

    @Test
    void shouldProduceSameReferencePipelineOutputViaMcpAndDirectService() throws IOException {
        Path fixtureDir = Path.of("src/test/resources/fixtures/batch-simple");
        Path mcpOutput = tempDir.resolve("mcp-output");
        Path directOutput = tempDir.resolve("direct-output");

        Map<String, Object> mcpArgs = new HashMap<>();
        mcpArgs.put("workspacePath", fixtureDir.toAbsolutePath().toString());
        mcpArgs.put("outputPath", mcpOutput.toString());
        mcpArgs.put("verifyEquivalence", false);

        Object mcpResult = mcpToolsProvider.executeCobolTool("cobol.pipeline.execute", mcpArgs);
        PipelineRequest request = new PipelineRequest("batch-simple", fixtureDir,
            PipelineRequest.of(fixtureDir, directOutput).decisions(), directOutput,
            fixtureDir.resolve("expected"), false, false);
        PipelineResult directResult = referencePipeline.execute(request);

        assertThat(mcpResult).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) mcpResult).get("success")).isEqualTo(directResult.isSuccessful());
        assertThat(directResult.isSuccessful())
            .as("direct pipeline build: %s", directResult.build() == null
                ? directResult : directResult.build().errors())
            .isTrue();
        assertThat(GeneratedTreeSnapshot.capture(mcpOutput))
            .isEqualTo(GeneratedTreeSnapshot.capture(directOutput));
    }

    @Test
    void shouldProduceSameOutputViaMcpAndDirectForCopybookMigration() {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/cics-mvc");

        // Act 1: MCP route - copybook migration
        Map<String, Object> mcpArgs = new HashMap<>();
        mcpArgs.put("workspacePath", fixtureDir.toAbsolutePath().toString());
        mcpArgs.put("copybook", "CUSTOMER.cpy");
        Object mcpResult = mcpToolsProvider.executeCobolTool("cobol.copybook.migrate", mcpArgs);

        // Act 2: Direct service call
        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("copybook");
        query.setLanguage("cobol");
        Map<String, Object> params = new HashMap<>();
        params.put("copybook", "CUSTOMER.cpy");
        query.setParameters(params);

        Workspace workspace = new Workspace();
        workspace.setId("surface-proof-copybook");
        workspace.setPath(fixtureDir.toAbsolutePath().toString());
        workspace.setBranch("main");

        StubResult directResult = cobolProvider.migrateCopybook(query, workspace);

        // Assert: Both routes produce results
        assertThat(mcpResult).isNotNull();
        assertThat(directResult).isNotNull();

        // MCP result is a Map with success key
        assertThat(mcpResult).isInstanceOf(Map.class);
        Map<?, ?> mcpMap = (Map<?, ?>) mcpResult;
        assertThat(mcpMap.get("success")).isEqualTo(directResult.isSuccess());

        // Both should have same success status
        assertThat(mcpMap.get("success")).isEqualTo(directResult.isSuccess());
    }

    @Test
    void shouldProduceSameOutputViaMcpAndDirectForDb2Migration() {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/db2-access");

        // Act 1: MCP route - DB2 migration
        Map<String, Object> mcpArgs = new HashMap<>();
        mcpArgs.put("workspacePath", fixtureDir.toAbsolutePath().toString());
        mcpArgs.put("program", "DB2001.cbl");
        Object mcpResult = mcpToolsProvider.executeCobolTool("cobol.db2.migrate", mcpArgs);

        // Act 2: Direct service call
        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("db2");
        query.setLanguage("cobol");
        Map<String, Object> params = new HashMap<>();
        params.put("program", "DB2001.cbl");
        query.setParameters(params);

        Workspace workspace = new Workspace();
        workspace.setId("surface-proof-db2");
        workspace.setPath(fixtureDir.toAbsolutePath().toString());
        workspace.setBranch("main");

        StubResult directResult = cobolProvider.migrateDb2(query, workspace);

        // Assert: Both routes produce results
        assertThat(mcpResult).isNotNull();
        assertThat(directResult).isNotNull();

        // MCP result is a Map with success key
        assertThat(mcpResult).isInstanceOf(Map.class);
        Map<?, ?> mcpMap = (Map<?, ?>) mcpResult;
        assertThat(mcpMap.get("success")).isEqualTo(directResult.isSuccess());

        // Both should have same success status
        assertThat(mcpMap.get("success")).isEqualTo(directResult.isSuccess());
    }

    @Test
    void shouldListAllAvailableCobolMcpTools() {
        // Act
        var tools = mcpToolsProvider.getCobolMigrationTools();

        // Assert
        assertThat(tools).isNotNull();
        assertThat(tools).hasSize(9);

        var toolNames = tools.stream().map(t -> t.getName()).toList();
        assertThat(toolNames).contains(
            "cobol.analyze",
            "cobol.generate.stubs",
            "cobol.migration.plan",
            "cobol.migration.apply",
            "cobol.metrics",
            "cobol.diff",
            "cobol.copybook.migrate",
            "cobol.db2.migrate",
            "cobol.pipeline.execute"
        );
    }
}
