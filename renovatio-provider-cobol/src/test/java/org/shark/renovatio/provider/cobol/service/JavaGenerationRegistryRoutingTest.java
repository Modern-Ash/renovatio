package org.shark.renovatio.provider.cobol.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.core.service.TargetEmitterRegistry;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
