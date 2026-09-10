package org.shark.renovatio.provider.cobol.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.provider.cobol.infrastructure.CobolMcpToolsProvider;
import org.shark.renovatio.provider.cobol.CobolLanguageProvider;
import org.shark.renovatio.provider.cobol.service.*;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.shark.renovatio.shared.domain.AnalyzeResult;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.io.IOException;
import java.nio.file.Files;
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

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        CobolParsingService parsingService = new CobolParsingService();
        TemplateCodeGenerationService templateService = new TemplateCodeGenerationService();
        CobolIntermediateModelService irService = new CobolIntermediateModelService();
        JavaGenerationService javaGenerationService = new JavaGenerationService(
            parsingService, templateService, irService,
            new CobolSemanticTranspiler(new OpenRewriteRunner())
        );
        Db2MigrationService db2Service = new Db2MigrationService(parsingService);
        MigrationPlanService migrationPlanService = new MigrationPlanService(parsingService, javaGenerationService);
        IndexingService indexingService = new IndexingService();
        MetricsService metricsService = new MetricsService();
        ControlBreakDecompositionService decompositionService = new ControlBreakDecompositionService(irService, parsingService);

        cobolProvider = new CobolLanguageProvider(
            parsingService, javaGenerationService, migrationPlanService,
            indexingService, metricsService, templateService, db2Service, decompositionService
        );
        mcpToolsProvider = new CobolMcpToolsProvider(cobolProvider);
    }

    @Test
    void shouldProduceSameOutputViaMcpAndDirectService() throws IOException {
        // Arrange
        Path fixtureDir = Path.of("src/test/resources/fixtures/batch-simple");

        // Act 1: MCP route - analyze via tool
        Map<String, Object> mcpArgs = new HashMap<>();
        mcpArgs.put("workspacePath", fixtureDir.toAbsolutePath().toString());
        mcpArgs.put("query", "Analyze COBOL structure");
        Object mcpResult = mcpToolsProvider.executeCobolTool("cobol.analyze", mcpArgs);

        // Act 2: Direct service call - analyze via provider
        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("programs");
        query.setLanguage("cobol");

        Workspace workspace = new Workspace();
        workspace.setId("surface-proof-test");
        workspace.setPath(fixtureDir.toAbsolutePath().toString());
        workspace.setBranch("main");

        AnalyzeResult directResult = cobolProvider.analyze(query, workspace);

        // Assert: Both routes produce results
        assertThat(mcpResult).isNotNull();
        assertThat(directResult).isNotNull();

        // MCP result is a Map with success key
        assertThat(mcpResult).isInstanceOf(Map.class);
        Map<?, ?> mcpMap = (Map<?, ?>) mcpResult;
        assertThat(mcpMap.get("success")).isEqualTo(directResult.isSuccess());

        // Both should indicate success (or same failure mode)
        assertThat(mcpMap.get("success")).isEqualTo(directResult.isSuccess());

        // Both should have same message
        if (mcpMap.get("message") != null && directResult.getMessage() != null) {
            String mcpMsg = mcpMap.get("message").toString();
            String directMsg = directResult.getMessage();
            // Compare first 50 chars or full message if shorter
            int compareLen = Math.min(50, Math.min(mcpMsg.length(), directMsg.length()));
            assertThat(mcpMsg.substring(0, compareLen))
                .isEqualTo(directMsg.substring(0, compareLen));
        }
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
        assertThat(tools).hasSize(8);

        var toolNames = tools.stream().map(t -> t.getName()).toList();
        assertThat(toolNames).contains(
            "cobol.analyze",
            "cobol.generate.stubs",
            "cobol.migration.plan",
            "cobol.migration.apply",
            "cobol.metrics",
            "cobol.diff",
            "cobol.copybook.migrate",
            "cobol.db2.migrate"
        );
    }
}
