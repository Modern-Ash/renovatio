package org.shark.renovatio.provider.java.emission;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;
import org.shark.renovatio.shared.emission.EmittedArtifact;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JavaEmitterTest {
    @Test
    void supportsOnlyJavaAndRejectsProfileMismatch() {
        JavaEmitter emitter = new JavaEmitter((model, profile) -> EmittedArtifacts.of(
                List.of(EmittedArtifact.utf8("A.java", "A"))));
        TargetModel model = model();
        assertTrue(emitter.supports(MigrationProfile.Language.JAVA));
        assertFalse(emitter.supports(MigrationProfile.Language.NODE));
        assertEquals("A", emitter.emit(model, model.profile()).artifacts().get(0).utf8Text());
        MigrationProfile different = new MigrationProfile(model.profile().schemaVersion(), model.profile().extensions(),
                model.profile().target(), model.profile().architecture(), new MigrationProfile.Runtime(MigrationProfile.Framework.NONE),
                model.profile().persistence(), model.profile().style(), model.profile().llm());
        assertThrows(IllegalArgumentException.class, () -> emitter.emit(model, different));
    }

    @Test
    void enabledDocumentationDecoratesJavaUnitsAtTheDeclarationBoundaryOnly() {
        JavaEmitter emitter = new JavaEmitter((model, profile) -> EmittedArtifacts.of(List.of(
                EmittedArtifact.utf8("A.java", "package example;\n\nimport java.util.List;\n\n@Service\npublic class A {}\n"),
                EmittedArtifact.utf8("B.java", "public interface B {}\n"),
                EmittedArtifact.utf8("metadata.json", "{}"))));
        TargetModel model = model(new MigrationProfile("1", Map.of("documentation.enabled", true),
                null, null, null, null, null, null));

        Map<String, String> files = emitter.emit(model, model.profile()).utf8TextByPath();

        assertTrue(files.get("A.java").contains("import java.util.List;\n\n/**\n * Migrated from COBOL program TEST"));
        assertTrue(files.get("A.java").contains("*/\n@Service\npublic class A"));
        assertTrue(files.get("B.java").startsWith("/**\n * Migrated from COBOL program TEST"));
        assertEquals("{}", files.get("metadata.json"));
    }

    private static TargetModel model() {
        return model(MigrationProfiles.emptyOverlay());
    }

    private static TargetModel model(MigrationProfile overlay) {
        SourceSpan span = new SourceSpan("src/program.cob", 1, 1, 1, 9);
        SourceProvenance provenance = new SourceProvenance("src/program.cob", "0".repeat(64),
                "COBOL", Optional.empty(), List.of());
        SemanticProgram program = new SemanticProgram("1", SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.PROGRAM, "program", span), "TEST", provenance,
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
        return TargetModel.from(program, MigrationProfiles.effective(overlay,
                Map.of(), Map.of(), List.of()));
    }
}
