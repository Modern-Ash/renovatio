package org.shark.renovatio.provider.cobol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.core.service.TargetEmitterRegistry;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.provider.cobol.service.*;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobolLanguageProviderEmitterRoutingTest {
    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. ROUTING.
            PROCEDURE DIVISION.
            MAIN.
                STOP RUN.
            """;

    @Test
    void unavailableTargetStopsEveryJavaProducingToolBeforeGeneration(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("routing.cbl"), COBOL);
        Files.writeString(root.resolve("record.cpy"), "01 RECORD-ITEM.\n  05 NAME PIC X(10).\n");
        Dependencies dependencies = dependencies();
        CobolLanguageProvider provider = provider(dependencies);
        Workspace workspace = new Workspace("test", root.toString(), "main");
        NqlQuery copybook = query(Map.of("copybook", "record.cpy"));
        NqlQuery db2 = query(Map.of("program", "routing.cbl"));
        var node = nodeProfile();

        assertUnavailable(() -> provider.migrateCopybook(copybook, workspace, node));
        assertUnavailable(() -> provider.migrateDb2(db2, workspace, node));
        assertUnavailable(() -> provider.decomposeControlBreaks(workspace, node));

    }

    @Test
    void generateStubsResolvesAndPreservesProjectEffectiveProfileOnce(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("routing.cbl"), COBOL);
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        TemplateCodeGenerationService templates = new TemplateCodeGenerationService();
        CobolIntermediateModelService models = new CobolIntermediateModelService();
        AtomicInteger resolutions = new AtomicInteger();
        JavaGenerationService generation = new JavaGenerationService(parsing, templates, models,
                new CobolSemanticTranspiler(new OpenRewriteRunner()),
                new ObjectMapper().findAndRegisterModules(), true, new TargetEmitterRegistry(List.of()), projectId -> {
                    assertEquals("project-42", projectId);
                    resolutions.incrementAndGet();
                    return nodeProfile();
                });
        CobolLanguageProvider provider = new CobolLanguageProvider(parsing, generation,
                new MigrationPlanService(parsing, generation), new IndexingService(), new MetricsService(), templates,
                new Db2MigrationService(parsing), new ControlBreakDecompositionService(models, parsing));

        assertUnavailable(() -> provider.generateStubs(new NqlQuery(),
                new Workspace("project-42", root.toString(), "main")));
        assertEquals(1, resolutions.get());
    }

    private static void assertUnavailable(org.junit.jupiter.api.function.Executable action) {
        var error = assertThrows(TargetEmitterRegistry.TargetEmitterUnavailableException.class, action);
        assertEquals("TARGET_EMITTER_UNAVAILABLE", error.code());
        assertEquals(MigrationProfile.Language.NODE, error.requestedTarget());
        assertEquals(List.of(MigrationProfile.Language.JAVA), error.availableTargets());
    }

    private static org.shark.renovatio.profile.MigrationProfiles.EffectiveProfile nodeProfile() {
        MigrationProfile overlay = new MigrationProfile("1", Map.of(),
                new MigrationProfile.Target(MigrationProfile.Language.NODE, "20"),
                null, null, null, null, null);
        return new DecisionResolver().resolve(overlay, List.of());
    }

    private static NqlQuery query(Map<String, Object> parameters) {
        NqlQuery query = new NqlQuery();
        query.setParameters(parameters);
        return query;
    }

    private static CobolLanguageProvider provider(Dependencies dependencies) {
        return new CobolLanguageProvider(dependencies.parsing(), dependencies.generation(),
                new MigrationPlanService(dependencies.parsing(), dependencies.generation()),
                new IndexingService(), new MetricsService(),
                dependencies.templates(), dependencies.db2(), dependencies.decomposition());
    }

    private static Dependencies dependencies() {
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        TemplateCodeGenerationService templates = new TemplateCodeGenerationService();
        CobolIntermediateModelService models = new CobolIntermediateModelService();
        Db2MigrationService db2 = new Db2MigrationService(parsing);
        ControlBreakDecompositionService decomposition = new ControlBreakDecompositionService(models, parsing);
        JavaGenerationService generation = new JavaGenerationService(parsing, templates,
                models, new CobolSemanticTranspiler(new OpenRewriteRunner()),
                new ObjectMapper().findAndRegisterModules(), true);
        return new Dependencies(parsing, generation, templates, db2, decomposition);
    }

    private record Dependencies(CobolParsingService parsing, JavaGenerationService generation,
                                TemplateCodeGenerationService templates, Db2MigrationService db2,
                                ControlBreakDecompositionService decomposition) { }
}
