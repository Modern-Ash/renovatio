package org.shark.renovatio.provider.cobol.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.core.service.TargetEmitterRegistry;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.domain.StubResult;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class JavaGenerationRegistryRoutingTest {
    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. ROUTED.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(20).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    @Test
    void defaultRegistryRoutePreservesLegacyArtifactKeysAndBytes(@TempDir Path workspacePath) throws Exception {
        Files.writeString(workspacePath.resolve("routed.cob"), COBOL);
        var dependencies = dependencies();
        JavaGenerationService legacy = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), false);
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true);
        Workspace workspace = new Workspace("test", workspacePath.toString(), "main");
        NqlQuery query = new NqlQuery();

        var baseline = legacy.generateInterfaceStubs(query, workspace);
        var actual = routed.generateInterfaceStubs(query, workspace);

        assertTrue(baseline.isSuccess(), baseline.getMessage());
        assertTrue(actual.isSuccess(), actual.getMessage());
        assertEquals(baseline.getGeneratedCode().keySet(), actual.getGeneratedCode().keySet());
        baseline.getGeneratedCode().forEach((path, content) -> assertEquals(content,
                actual.getGeneratedCode().get(path), path));
        assertEquals("JAVA", actual.getTargetLanguage());
    }

    @Test
    void nodeTargetFailsBeforeLegacyRendererRuns(@TempDir Path workspacePath) throws Exception {
        Files.writeString(workspacePath.resolve("routed.cob"), COBOL);
        var dependencies = dependencies();
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true);
        Workspace workspace = new Workspace("test", workspacePath.toString(), "main");
        MigrationProfile overlay = new MigrationProfile("1", Map.of(),
                new MigrationProfile.Target(MigrationProfile.Language.NODE, "20"),
                null, null, null, null, null);
        var effective = new DecisionResolver().resolve(overlay, List.of());

        var error = assertThrows(TargetEmitterRegistry.TargetEmitterUnavailableException.class,
                () -> routed.generateInterfaceStubs(new NqlQuery(), workspace, effective));
        assertEquals("TARGET_EMITTER_UNAVAILABLE", error.code());
        assertEquals(MigrationProfile.Language.NODE, error.requestedTarget());
        assertEquals(List.of(MigrationProfile.Language.JAVA), error.availableTargets());
        assertFalse(Files.exists(workspacePath.resolve("generated-java-stubs")));
    }

    @Test
    void routedAggregationRejectsDuplicateArtifactPaths(@TempDir Path workspacePath) throws Exception {
        Path first = Files.createDirectories(workspacePath.resolve("one"));
        Path second = Files.createDirectories(workspacePath.resolve("two"));
        Files.writeString(first.resolve("same.cob"), COBOL.replace("ROUTED", "FIRST"));
        Files.writeString(second.resolve("same.cob"), COBOL.replace("ROUTED", "SECOND"));
        var dependencies = dependencies();
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true);

        var result = routed.generateInterfaceStubs(new NqlQuery(),
                new Workspace("test", workspacePath.toString(), "main"));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("duplicate artifact path"), result.getMessage());
    }

    @Test
    void applicationRegistryEmitsEveryProgramWithItsOwnSemanticEnvelope(@TempDir Path workspacePath) throws Exception {
        Files.writeString(workspacePath.resolve("first.cob"), COBOL.replace("ROUTED", "FIRST"));
        Files.writeString(workspacePath.resolve("second.cob"), COBOL.replace("ROUTED", "SECOND"));
        var dependencies = dependencies();
        List<TargetModel> received = new ArrayList<>();
        TargetEmitter node = new TargetEmitter() {
            @Override
            public boolean supports(MigrationProfile.Language target) {
                return target == MigrationProfile.Language.NODE;
            }

            @Override
            public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) {
                received.add(model);
                String id = model.semanticProgram().programId().toLowerCase();
                return EmittedArtifacts.of(List.of(EmittedArtifact.utf8(id + ".js", "export const id = '" + id + "';")));
            }
        };
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true,
                new TargetEmitterRegistry(List.of(node)), ignored -> nodeProfile());

        StubResult result = routed.generateInterfaceStubs(new NqlQuery(),
                new Workspace("test", workspacePath.toString(), "main"));

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("NODE", result.getTargetLanguage());
        assertEquals(List.of("FIRST", "SECOND"), received.stream()
                .map(model -> model.semanticProgram().programId()).sorted().toList());
        assertEquals(List.of("first.cob", "second.cob"), received.stream()
                .map(model -> model.sourceProvenance().sourcePath()).sorted().toList());
        assertEquals(1, received.stream().map(model -> model.targetStructure().requestHash()).distinct().count());
        assertEquals(2, received.stream().map(model -> model.targetStructure().moduleId()).distinct().count());
        assertTrue(received.stream().allMatch(model -> !model.targetStructure().componentIds().isEmpty()));
        assertEquals(Map.of("first.js", "export const id = 'first';",
                "second.js", "export const id = 'second';"), result.getGeneratedCode());
        assertFalse(Files.exists(workspacePath.resolve("generated-java-stubs")));
    }

    @Test
    void copybookProjectionFailureDoesNotInvokeLegacyRenderer(@TempDir Path workspacePath) throws Exception {
        Path copybook = Files.writeString(workspacePath.resolve("broken.cpy"), "01 BROKEN PIC X.\n");
        var dependencies = dependencies();
        CobolIntermediateModelService failingModels = new CobolIntermediateModelService() {
            @Override
            public org.shark.renovatio.cobol.ir.model.CobolIntermediateModel parse(Path ignored) {
                throw new IllegalStateException("projection rejected");
            }
        };
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                failingModels, dependencies.transpiler(), dependencies.mapper(), true,
                new TargetEmitterRegistry(List.of()), ignored -> javaProfile());
        AtomicBoolean rendered = new AtomicBoolean();

        StubResult result = routed.emitThroughRegistry(copybook, new NqlQuery(),
                new Workspace("test", workspacePath.toString(), "main"), javaProfile(), semantic -> {
                    rendered.set(true);
                    return new StubResult(true, "should not run");
                });

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("projection rejected"), result.getMessage());
        assertFalse(rendered.get());
    }

    private static MigrationProfiles.EffectiveProfile nodeProfile() {
        MigrationProfile overlay = new MigrationProfile("1", Map.of(),
                new MigrationProfile.Target(MigrationProfile.Language.NODE, "20"),
                null, null, null, null, null);
        return new DecisionResolver().resolve(overlay, List.of());
    }

    private static MigrationProfiles.EffectiveProfile javaProfile() {
        return new DecisionResolver().resolve(MigrationProfiles.emptyOverlay(), List.of());
    }

    private static Dependencies dependencies() {
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        TemplateCodeGenerationService templates = new TemplateCodeGenerationService();
        CobolIntermediateModelService models = new CobolIntermediateModelService();
        CobolSemanticTranspiler transpiler = new CobolSemanticTranspiler(new OpenRewriteRunner());
        return new Dependencies(parsing, templates, models, transpiler,
                new ObjectMapper().findAndRegisterModules());
    }

    private record Dependencies(CobolParsingService parsing, TemplateCodeGenerationService templates,
                                CobolIntermediateModelService models, CobolSemanticTranspiler transpiler,
                                ObjectMapper mapper) { }
}
