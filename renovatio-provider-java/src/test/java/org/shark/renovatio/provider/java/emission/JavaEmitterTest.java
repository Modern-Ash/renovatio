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
}
