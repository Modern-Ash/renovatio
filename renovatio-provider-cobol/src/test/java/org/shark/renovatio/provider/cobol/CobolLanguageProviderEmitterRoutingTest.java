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
import org.shark.renovatio.shared.domain.Scope;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.emission.EmittedArtifact;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.TargetEmitter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void planApplicationPropagatesUnavailableTarget(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("routing.cbl"), COBOL);
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        TemplateCodeGenerationService templates = new TemplateCodeGenerationService();
        CobolIntermediateModelService models = new CobolIntermediateModelService();
        JavaGenerationService generation = new JavaGenerationService(parsing, templates, models,
                new CobolSemanticTranspiler(new OpenRewriteRunner()),
                new ObjectMapper().findAndRegisterModules(), true, new TargetEmitterRegistry(List.of()),
                ignored -> nodeProfile());
        CobolLanguageProvider provider = new CobolLanguageProvider(parsing, generation,
                new MigrationPlanService(parsing, generation), new IndexingService(), new MetricsService(), templates,
                new Db2MigrationService(parsing), new ControlBreakDecompositionService(models, parsing));
        Workspace workspace = new Workspace("project-42", root.toString(), "main");
        var plan = provider.plan(new NqlQuery(), new Scope(), workspace);

        assertUnavailable(() -> provider.apply(plan.getPlanId(), false, workspace));
    }

    @Test
    void planApplicationFailsWhenRegisteredEmitterThrows(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("routing.cbl"), COBOL);
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        TemplateCodeGenerationService templates = new TemplateCodeGenerationService();
        CobolIntermediateModelService models = new CobolIntermediateModelService();
        TargetEmitter failingEmitter = new TargetEmitter() {
            @Override
            public boolean supports(MigrationProfile.Language target) {
                return target == MigrationProfile.Language.NODE;
            }

            @Override
            public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) {
                throw new IllegalStateException("emitter exploded");
            }
        };
        JavaGenerationService generation = new JavaGenerationService(parsing, templates, models,
                new CobolSemanticTranspiler(new OpenRewriteRunner()),
                new ObjectMapper().findAndRegisterModules(), true,
                new TargetEmitterRegistry(List.of(failingEmitter)), ignored -> nodeProfile());
        CobolLanguageProvider provider = new CobolLanguageProvider(parsing, generation,
                new MigrationPlanService(parsing, generation), new IndexingService(), new MetricsService(), templates,
                new Db2MigrationService(parsing), new ControlBreakDecompositionService(models, parsing));
        Workspace workspace = new Workspace("project-42", root.toString(), "main");
        var plan = provider.plan(new NqlQuery(), new Scope(), workspace);

        var result = provider.apply(plan.getPlanId(), false, workspace);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("emitter exploded"), result.getMessage());
    }

    @Test
    void controlBreakRouteEmitsEachProgramWithItsOwnEnvelope(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("first.cbl"), COBOL.replace("ROUTING", "FIRST"));
        Files.writeString(root.resolve("second.cbl"), COBOL.replace("ROUTING", "SECOND"));
        List<TargetModel> received = new ArrayList<>();
        CobolLanguageProvider provider = providerWithNodeEmitter(received);

        StubResult result = provider.decomposeControlBreaks(
                new Workspace("test", root.toString(), "main"), nodeProfile());

        assertEquals(List.of("FIRST", "SECOND"), received.stream()
                .map(model -> model.semanticProgram().programId()).sorted().toList());
        assertEquals(List.of("first.cbl", "second.cbl"), received.stream()
                .map(model -> model.sourceProvenance().sourcePath()).sorted().toList());
        assertEquals(List.of("first.js", "second.js"), result.getGeneratedCode().keySet().stream().sorted().toList());
    }

    @Test
    void controlBreakRouteDoesNotWriteBeforeAggregateValidation(@TempDir Path root) throws Exception {
        String controlBreak = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAME.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-KEY PIC X(10).
                01 WS-PREV-KEY PIC X(10).
                PROCEDURE DIVISION.
                MAIN.
                    OPEN INPUT DATA-FILE.
                    READ DATA-FILE.
                    IF WS-KEY NOT = WS-PREV-KEY
                        PERFORM BREAK-PARA
                    END-IF.
                    CLOSE DATA-FILE.
                    STOP RUN.
                BREAK-PARA.
                    MOVE WS-KEY TO WS-PREV-KEY.
                """;
        Files.writeString(root.resolve("first.cbl"), controlBreak);
        Files.writeString(root.resolve("second.cbl"), controlBreak);
        CobolLanguageProvider provider = provider(dependencies());

        StubResult result = provider.decomposeControlBreaks(new Workspace("test", root.toString(), "main"));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("duplicate artifact path"), result.getMessage());
        assertFalse(Files.exists(root.resolve("generated-decomposed")));
    }

    @Test
    void standaloneCopybookProjectsFieldsForRegisteredEmitter(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("customer.cpy"), """
                01 CUSTOMER-RECORD.
                   05 CUSTOMER-NAME PIC X(20).
                   05 BALANCE PIC S9(7)V99.
                """);
        List<TargetModel> received = new ArrayList<>();
        CobolLanguageProvider provider = providerWithNodeEmitter(received);

        StubResult result = provider.migrateCopybook(query(Map.of("copybook", "customer.cpy")),
                new Workspace("test", root.toString(), "main"), nodeProfile());

        assertEquals(1, received.size());
        assertEquals(List.of("BALANCE", "CUSTOMER-NAME"), received.get(0).semanticProgram().types().stream()
                .map(type -> type.symbol()).sorted().toList());
        assertEquals("customer.cpy", received.get(0).sourceProvenance().sourcePath());
        assertEquals("NODE", result.getTargetLanguage());
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

    private static CobolLanguageProvider providerWithNodeEmitter(List<TargetModel> received) {
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        TemplateCodeGenerationService templates = new TemplateCodeGenerationService();
        CobolIntermediateModelService models = new CobolIntermediateModelService();
        TargetEmitter nodeEmitter = new TargetEmitter() {
            @Override
            public boolean supports(MigrationProfile.Language target) {
                return target == MigrationProfile.Language.NODE;
            }

            @Override
            public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) {
                received.add(model);
                String id = model.semanticProgram().programId().toLowerCase();
                return EmittedArtifacts.of(List.of(EmittedArtifact.utf8(id + ".js", "export {};")));
            }
        };
        JavaGenerationService generation = new JavaGenerationService(parsing, templates, models,
                new CobolSemanticTranspiler(new OpenRewriteRunner()),
                new ObjectMapper().findAndRegisterModules(), true,
                new TargetEmitterRegistry(List.of(nodeEmitter)), ignored -> nodeProfile());
        return new CobolLanguageProvider(parsing, generation, new MigrationPlanService(parsing, generation),
                new IndexingService(), new MetricsService(), templates, new Db2MigrationService(parsing),
                new ControlBreakDecompositionService(models, parsing));
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
