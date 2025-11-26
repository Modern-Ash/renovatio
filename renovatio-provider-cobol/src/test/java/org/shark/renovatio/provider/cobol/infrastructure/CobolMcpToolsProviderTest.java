package org.shark.renovatio.provider.cobol.infrastructure;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.provider.cobol.CobolLanguageProvider;
import org.shark.renovatio.provider.cobol.domain.CobolMcpTool;
import org.shark.renovatio.provider.cobol.service.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CobolMcpToolsProviderTest {

    @Test
    void getCobolMigrationTools_exposesExpectedTools() {
        // Build a minimal provider
        CobolParsingService parsingService = new CobolParsingService();
        TemplateCodeGenerationService templateService = new TemplateCodeGenerationService();
        org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService irService = new org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService();
        JavaGenerationService javaGenerationService = new JavaGenerationService(parsingService, templateService, irService, new org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler(new org.shark.renovatio.provider.java.OpenRewriteRunner()));
        Db2MigrationService db2Service = new Db2MigrationService(parsingService);
        MigrationPlanService migrationPlanService = new MigrationPlanService(parsingService, javaGenerationService);
        IndexingService indexingService = new IndexingService();
        MetricsService metricsService = new MetricsService();
        ControlBreakDecompositionService decompositionService = new ControlBreakDecompositionService(irService, parsingService);
        CobolLanguageProvider provider = new CobolLanguageProvider(
                parsingService, javaGenerationService, migrationPlanService,
                indexingService, metricsService, templateService, db2Service, decompositionService);

        CobolMcpToolsProvider toolsProvider = new CobolMcpToolsProvider(provider);
        List<CobolMcpTool> tools = toolsProvider.getCobolMigrationTools();

        assertNotNull(tools);
        assertTrue(tools.size() >= 8);

        Set<String> names = tools.stream().map(CobolMcpTool::getName).collect(Collectors.toSet());
        assertTrue(names.contains("cobol.analyze"));
        assertTrue(names.contains("cobol.generate.stubs"));
        assertTrue(names.contains("cobol.migration.plan"));
        assertTrue(names.contains("cobol.migration.apply"));
        assertTrue(names.contains("cobol.metrics"));
        assertTrue(names.contains("cobol.diff"));
        assertTrue(names.contains("cobol.copybook.migrate"));
        assertTrue(names.contains("cobol.db2.migrate"));
    }
}
