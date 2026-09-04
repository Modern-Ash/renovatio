package org.shark.renovatio.emitter.node;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;
import org.shark.renovatio.shared.emission.EmittedArtifact;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.emission.TargetStructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NodeEmitterTest {
    @Test
    void supportsOnlyNodeAndRejectsProfileMismatch() {
        NodeEmitter emitter = new NodeEmitter((model, profile) -> EmittedArtifacts.of(
                List.of(EmittedArtifact.utf8("main.ts", "app"))));
        TargetModel model = model();
        assertTrue(emitter.supports(MigrationProfile.Language.NODE));
        assertFalse(emitter.supports(MigrationProfile.Language.JAVA));
        assertFalse(emitter.supports(MigrationProfile.Language.PYTHON));
        assertEquals("app", emitter.emit(model, model.profile()).artifacts().get(0).utf8Text());
        MigrationProfile different = new MigrationProfile(model.profile().schemaVersion(), model.profile().extensions(),
                model.profile().target(), model.profile().architecture(), new MigrationProfile.Runtime(MigrationProfile.Framework.NONE),
                model.profile().persistence(), model.profile().style(), model.profile().llm());
        assertThrows(IllegalArgumentException.class, () -> emitter.emit(model, different));
    }

    @Test
    void defaultRendererEmitsCanonicalProgramPathsAndStableProjectFiles() {
        TargetModel first = model("FIRST", List.of(
                "src/first/domain/first.entity.ts",
                "src/first/domain/first.repository.ts",
                "src/first/domain/first.service.ts",
                "src/first/api/first.controller.ts"));
        TargetModel second = model("SECOND", List.of(
                "src/second/domain/second.service.ts"));
        DefaultNodeRenderer renderer = new DefaultNodeRenderer();

        Map<String, String> firstFiles = renderer.render(first, first.profile()).utf8TextByPath();
        Map<String, String> secondFiles = renderer.render(second, second.profile()).utf8TextByPath();

        assertTrue(firstFiles.keySet().containsAll(first.targetStructure().artifactPaths()));
        assertTrue(secondFiles.keySet().containsAll(second.targetStructure().artifactPaths()));
        assertTrue(firstFiles.get("src/first/domain/first.service.ts").contains("class FirstService"));
        assertTrue(firstFiles.get("src/first/domain/first.entity.ts").contains("interface FirstEntity"));
        assertTrue(firstFiles.get("src/first/domain/first.repository.ts").contains("interface FirstRepository"));
        assertTrue(firstFiles.get("src/first/api/first.controller.ts").contains("firstController"));
        for (String shared : List.of("src/main.ts", "package.json", "tsconfig.json")) {
            assertEquals(firstFiles.get(shared), secondFiles.get(shared), shared);
        }
        assertFalse(firstFiles.get("src/main.ts").contains("FIRST"));
        assertFalse(firstFiles.get("package.json").contains("first"));
    }

    @Test
    void enabledDocumentationDecoratesProgramUnitsButNotSharedProjectFiles() {
        TargetModel first = model("FIRST", List.of("src/first/domain/first.service.ts"),
                Map.of("documentation.enabled", true));
        TargetModel second = model("SECOND", List.of("src/second/domain/second.service.ts"),
                Map.of("documentation.enabled", true));
        DefaultNodeRenderer renderer = new DefaultNodeRenderer();

        Map<String, String> firstFiles = renderer.render(first, first.profile()).utf8TextByPath();
        Map<String, String> secondFiles = renderer.render(second, second.profile()).utf8TextByPath();

        assertTrue(firstFiles.get("src/first/domain/first.service.ts")
                .startsWith("/**\n * Migrated from COBOL program FIRST"));
        for (String shared : List.of("src/main.ts", "package.json", "tsconfig.json")) {
            assertEquals(firstFiles.get(shared), secondFiles.get(shared), shared);
            assertFalse(firstFiles.get(shared).contains("Migrated from COBOL"), shared);
        }
    }

    @Test
    void prismaProfileEmitsDeterministicSharedArtifacts() {
        TargetModel model = model("PRISMA", List.of("src/prisma/domain/prisma.service.ts"), Map.of(),
                MigrationProfile.PersistenceStrategy.PRISMA);
        Map<String, String> files = new DefaultNodeRenderer().render(model, model.profile()).utf8TextByPath();
        assertTrue(files.containsKey("prisma/schema.prisma"));
        assertTrue(files.containsKey("prisma/seed.ts"));
        assertTrue(files.get("prisma/schema.prisma").contains("model Prisma"));
    }

    private static TargetModel model() {
        SourceSpan span = new SourceSpan("src/program.cob", 1, 1, 1, 9);
        SourceProvenance provenance = new SourceProvenance("src/program.cob", "0".repeat(64),
                "COBOL", Optional.empty(), List.of());
        SemanticProgram program = new SemanticProgram("1", SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.PROGRAM, "program", span), "TEST", provenance,
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
        return TargetModel.from(program, MigrationProfiles.effective(MigrationProfiles.emptyOverlay(),
                Map.of(), Map.of(), List.of()));
    }

    private static TargetModel model(String programId, List<String> artifactPaths) {
        return model(programId, artifactPaths, Map.of());
    }

    private static TargetModel model(String programId, List<String> artifactPaths,
                                     Map<String, Object> extensions) {
        return model(programId, artifactPaths, extensions, null);
    }

    private static TargetModel model(String programId, List<String> artifactPaths,
                                     Map<String, Object> extensions,
                                     MigrationProfile.PersistenceStrategy persistenceStrategy) {
        SourceSpan span = new SourceSpan("src/" + programId.toLowerCase() + ".cob", 1, 1, 1, 9);
        SourceProvenance provenance = new SourceProvenance(span.sourcePath(), "0".repeat(64),
                "COBOL", Optional.empty(), List.of());
        SemanticProgram program = new SemanticProgram("1", SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.PROGRAM, "program", span), programId, provenance,
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
        MigrationProfile overlay = new MigrationProfile("1", extensions,
                new MigrationProfile.Target(MigrationProfile.Language.NODE, "20"),
                null, null, persistenceStrategy == null ? null : new MigrationProfile.Persistence(persistenceStrategy,
                        MigrationProfile.TransactionBoundary.PROGRAM, Map.of()), null, null);
        var effective = MigrationProfiles.effective(overlay, Map.of(), Map.of(), List.of());
        return new TargetModel(program, effective.profile(), effective.resolvedDecisions(),
                effective.appliedDecisionIds(), effective.profileHash(), provenance,
                new TargetStructure("1", "1".repeat(64), "2".repeat(64),
                        effective.profile().architecture().style(), effective.profile().architecture().style(),
                        List.of(), artifactPaths, List.of()));
    }
}
