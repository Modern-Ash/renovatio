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
import org.shark.renovatio.shared.emission.TargetStructure;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.TargetEmitter;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.tools.ToolProvider;

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
    void hexagonalRouteUsesTheCanonicalLayeredManifest(@TempDir Path workspacePath) throws Exception {
        Files.writeString(workspacePath.resolve("routed.cob"), COBOL);
        var dependencies = dependencies();
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true,
                new TargetEmitterRegistry(List.of()), ignored -> hexagonalJavaProfile());

        StubResult result = routed.generateInterfaceStubs(new NqlQuery(),
                new Workspace("test", workspacePath.toString(), "main"));

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(List.of(
                "modules/routed/application/port/in/RoutedService.java",
                "modules/routed/application/service/RoutedServiceImpl.java",
                "modules/routed/domain/model/RoutedDTO.java"), result.getGeneratedCode().keySet().stream().toList());
        assertTrue(result.getGeneratedCode().values().stream().allMatch(value -> !value.isBlank()));
        for (String path : result.getGeneratedCode().keySet()) {
            assertEquals(result.getGeneratedCode().get(path),
                    Files.readString(workspacePath.resolve("generated-java-stubs").resolve(path)));
        }
    }

    @Test
    void previewUsesTheEmissionManifestWithoutWritingTheWorkspace(@TempDir Path workspacePath) throws Exception {
        Files.writeString(workspacePath.resolve("routed.cob"), COBOL);
        var dependencies = dependencies();
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true,
                new TargetEmitterRegistry(List.of()), ignored -> hexagonalJavaProfile());
        Workspace workspace = new Workspace("test", workspacePath.toString(), "main");

        var preview = routed.previewArchitecture(new NqlQuery(), workspace);

        assertEquals(List.of(
                "modules/routed/application/port/in/RoutedService.java",
                "modules/routed/application/service/RoutedServiceImpl.java",
                "modules/routed/domain/model/RoutedDTO.java"), preview.manifest().artifacts().stream()
                .map(artifact -> artifact.path()).toList());
        assertFalse(Files.exists(workspacePath.resolve("generated-java-stubs")));

        StubResult emitted = routed.generateInterfaceStubs(new NqlQuery(), workspace);
        assertTrue(emitted.isSuccess(), emitted.getMessage());
        assertEquals(preview.manifest().artifacts().stream().map(artifact -> artifact.path()).toList(),
                emitted.getGeneratedCode().keySet().stream().toList());
    }

    @Test
    void cicsControllerIsPlannedBeforeManifestValidation(@TempDir Path workspacePath) throws Exception {
        String cics = COBOL.replace("MOVE 'A' TO CUSTOMER-NAME.",
                "EXEC CICS LINK PROGRAM('BACKEND') END-EXEC.");
        Files.writeString(workspacePath.resolve("routed.cob"), cics);
        var dependencies = dependencies();
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true);
        Workspace workspace = new Workspace("test", workspacePath.toString(), "main");

        var preview = routed.previewArchitecture(new NqlQuery(), workspace);
        StubResult emitted = routed.generateInterfaceStubs(new NqlQuery(), workspace);

        assertTrue(emitted.isSuccess(), emitted.getMessage());
        assertTrue(preview.manifest().artifacts().stream()
                .anyMatch(artifact -> artifact.path().equals("RoutedCicsController.java")));
        assertEquals(preview.manifest().artifacts().stream().map(artifact -> artifact.path()).toList(),
                emitted.getGeneratedCode().keySet().stream().toList());
    }

    @Test
    void domainGroupingUsesCopybookRelationshipsFromSource(@TempDir Path workspacePath) throws Exception {
        String source = COBOL.replace("01 CUSTOMER-NAME PIC X(20).",
                "01 CUSTOMER-NAME PIC X(20).\n            COPY customer-rec.cpy.");
        Files.writeString(workspacePath.resolve("routed.cob"), source);
        Files.writeString(workspacePath.resolve("customer-rec.cpy"), "01 CUSTOMER-REC PIC X(20).\n");
        var dependencies = dependencies();
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true);
        Workspace workspace = new Workspace("test", workspacePath.toString(), "main");

        var preview = routed.previewArchitecture(new NqlQuery(), workspace, domainHexagonalProfile());
        StubResult emitted = routed.generateInterfaceStubs(new NqlQuery(), workspace, domainHexagonalProfile());

        assertEquals(List.of("customers"), preview.graph().modules().stream()
                .map(module -> module.name()).toList());
        assertTrue(emitted.isSuccess(), emitted.getMessage());
        assertEquals(preview.manifest().artifacts().stream().map(artifact -> artifact.path()).toList(),
                emitted.getGeneratedCode().keySet().stream().toList());
        assertTrue(emitted.getGeneratedCode().keySet().stream()
                .allMatch(path -> path.startsWith("modules/customers/")));
    }

    @Test
    void transactionScriptAndHexagonalLayoutsCompile(@TempDir Path root) throws Exception {
        Path transactionWorkspace = Files.createDirectories(root.resolve("transaction"));
        Path hexagonalWorkspace = Files.createDirectories(root.resolve("hexagonal"));
        Files.writeString(transactionWorkspace.resolve("routed.cob"), COBOL);
        Files.writeString(hexagonalWorkspace.resolve("routed.cob"), COBOL);
        var dependencies = dependencies();
        JavaGenerationService routed = new JavaGenerationService(dependencies.parsing(), dependencies.templates(),
                dependencies.models(), dependencies.transpiler(), dependencies.mapper(), true);

        StubResult transaction = routed.generateInterfaceStubs(new NqlQuery(),
                new Workspace("transaction", transactionWorkspace.toString(), "main"), javaProfile());
        StubResult hexagonal = routed.generateInterfaceStubs(new NqlQuery(),
                new Workspace("hexagonal", hexagonalWorkspace.toString(), "main"), hexagonalJavaProfile());

        assertTrue(transaction.isSuccess(), transaction.getMessage());
        assertTrue(hexagonal.isSuccess(), hexagonal.getMessage());
        compile(transactionWorkspace.resolve("generated-java-stubs"), root.resolve("transaction-classes"));
        compile(hexagonalWorkspace.resolve("generated-java-stubs"), root.resolve("hexagonal-classes"));
    }

    @Test
    void rejectsJavaOutputThatDoesNotMatchTheCanonicalManifest() {
        SourceSpan span = new SourceSpan("routed.cob", 1, 1, 1, 9);
        SourceProvenance provenance = new SourceProvenance("routed.cob", "0".repeat(64), "COBOL",
                java.util.Optional.empty(), List.of());
        SemanticProgram program = new SemanticProgram("1", SemanticProgram.Header.create("ROUTED",
                SemanticProgram.NodeKind.PROGRAM, "program", span), "ROUTED", provenance,
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(java.util.Optional.empty(), List.of(), List.of()), List.of());
        MigrationProfiles.EffectiveProfile effective = javaProfile();
        TargetModel model = new TargetModel(program, effective.profile(), effective.resolvedDecisions(),
                effective.appliedDecisionIds(), effective.profileHash(), provenance,
                new TargetStructure("1", "1".repeat(64), "2".repeat(64),
                        MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                        MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT, List.of(),
                        List.of("RoutedDTO.java"), List.of()));

        var error = assertThrows(JavaGenerationService.TargetManifestMismatchException.class,
                () -> JavaGenerationService.applyManifest(model,
                        EmittedArtifacts.fromUtf8(Map.of("Unexpected.java", "class Unexpected {}"))));
        assertTrue(error.getMessage().contains("TARGET_MANIFEST_MISMATCH"));
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

    private static MigrationProfiles.EffectiveProfile hexagonalJavaProfile() {
        MigrationProfile overlay = new MigrationProfile("1", Map.of(), null,
                new MigrationProfile.Architecture(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                        MigrationProfile.ModuleGrouping.BY_PROGRAM), null, null, null, null);
        return new DecisionResolver().resolve(overlay, List.of());
    }

    private static MigrationProfiles.EffectiveProfile domainHexagonalProfile() {
        MigrationProfile overlay = new MigrationProfile("1", Map.of("renovatio.architecture", Map.of(
                "domainCopybooks", Map.of("CUSTOMER-REC", "customers"))), null,
                new MigrationProfile.Architecture(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                        MigrationProfile.ModuleGrouping.BY_DOMAIN), null, null, null, null);
        return new DecisionResolver().resolve(overlay, List.of());
    }

    private static Dependencies dependencies() {
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        TemplateCodeGenerationService templates = new TemplateCodeGenerationService();
        CobolIntermediateModelService models = new CobolIntermediateModelService();
        CobolSemanticTranspiler transpiler = new CobolSemanticTranspiler(new OpenRewriteRunner());
        return new Dependencies(parsing, templates, models, transpiler,
                new ObjectMapper().findAndRegisterModules());
    }

    private static void compile(Path sources, Path classes) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK compiler is required");
        Files.createDirectories(classes);
        List<String> arguments = new ArrayList<>(List.of("-classpath", System.getProperty("java.class.path"),
                "-d", classes.toString()));
        try (var paths = Files.walk(sources)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted().map(Path::toString).forEach(arguments::add);
        }
        assertEquals(0, compiler.run(null, null, null, arguments.toArray(String[]::new)),
                "Generated layout did not compile: " + Arrays.toString(arguments.toArray()));
    }

    private record Dependencies(CobolParsingService parsing, TemplateCodeGenerationService templates,
                                CobolIntermediateModelService models, CobolSemanticTranspiler transpiler,
                                ObjectMapper mapper) { }
}
