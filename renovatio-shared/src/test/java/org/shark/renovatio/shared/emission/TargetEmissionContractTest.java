package org.shark.renovatio.shared.emission;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TargetEmissionContractTest {
    @Test
    void targetModelCopiesTheEffectiveEnvelope() {
        var effective = MigrationProfiles.effective(MigrationProfiles.emptyOverlay(),
                Map.of("b", "2", "a", "1"), Map.of(), List.of());
        TargetModel model = TargetModel.from(program(), effective);
        assertEquals(List.of("a", "b"), model.resolvedDecisions().keySet().stream().toList());
        assertEquals(effective.profileHash(), model.profileHash());
        assertEquals(effective.profile(), model.profile());
        assertEquals(model.semanticProgram().sourceProvenance(), model.sourceProvenance());
        assertEquals(MigrationProfile.Language.JAVA, model.targetLanguage());
        assertEquals(MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                model.targetStructure().effectiveStyle());
        assertTrue(model.targetStructure().artifactPaths().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> model.resolvedDecisions().put("c", "3"));
    }

    @Test
    void targetModelRejectsInvalidOrInconsistentEnvelopeValues() {
        SemanticProgram program = program();
        MigrationProfile defaults = MigrationProfiles.defaults();
        assertThrows(NullPointerException.class, () -> new TargetModel(null, defaults, Map.of(), List.of(),
                "0".repeat(64), program.sourceProvenance()));
        assertThrows(NullPointerException.class, () -> new TargetModel(program, null, Map.of(), List.of(),
                "0".repeat(64), program.sourceProvenance()));
        MigrationProfile missingTarget = new MigrationProfile("1", Map.of(), null, defaults.architecture(),
                defaults.runtime(), defaults.persistence(), defaults.style(), defaults.llm());
        assertThrows(IllegalArgumentException.class, () -> new TargetModel(program, missingTarget, Map.of(),
                List.of(), "0".repeat(64), program.sourceProvenance()));
        MigrationProfile missingLanguage = new MigrationProfile("1", Map.of(),
                new MigrationProfile.Target(null, "17"), defaults.architecture(), defaults.runtime(),
                defaults.persistence(), defaults.style(), defaults.llm());
        assertThrows(IllegalArgumentException.class, () -> new TargetModel(program, missingLanguage, Map.of(),
                List.of(), "0".repeat(64), program.sourceProvenance()));
        assertThrows(IllegalArgumentException.class, () -> new TargetModel(program, defaults, Map.of("", "x"),
                List.of(), "0".repeat(64), program.sourceProvenance()));
        assertThrows(IllegalArgumentException.class, () -> new TargetModel(program, defaults, Map.of("x", ""),
                List.of(), "0".repeat(64), program.sourceProvenance()));
        assertThrows(IllegalArgumentException.class, () -> new TargetModel(program, defaults, Map.of(),
                List.of("not-a-hash"), "0".repeat(64), program.sourceProvenance()));
        assertThrows(IllegalArgumentException.class, () -> new TargetModel(program, defaults, Map.of(), List.of(),
                "ABC", program.sourceProvenance()));
        SourceProvenance other = new SourceProvenance("src/other.cob", "1".repeat(64), "COBOL",
                Optional.empty(), List.of());
        assertThrows(IllegalArgumentException.class, () -> new TargetModel(program, defaults, Map.of(), List.of(),
                "0".repeat(64), other));
        assertThrows(NullPointerException.class, () -> TargetModel.from(program, null));
    }

    @Test
    void artifactsAreSortedImmutableAndDefensive() {
        byte[] bytes = "A".getBytes(StandardCharsets.UTF_8);
        EmittedArtifacts artifacts = EmittedArtifacts.of(List.of(
                EmittedArtifact.utf8("z/B.java", "B"), new EmittedArtifact("a/A.java", bytes)));
        bytes[0] = 'X';
        assertEquals(List.of("a/A.java", "z/B.java"), artifacts.artifacts().stream().map(EmittedArtifact::path).toList());
        assertEquals("A", artifacts.artifacts().get(0).utf8Text());
        byte[] returned = artifacts.artifacts().get(0).content(); returned[0] = 'X';
        assertEquals("A", artifacts.artifacts().get(0).utf8Text());
        assertEquals(new EmittedArtifact("a/A.java", new byte[]{'A'}), artifacts.artifacts().get(0));
        assertEquals(new EmittedArtifact("a/A.java", new byte[]{'A'}).hashCode(),
                artifacts.artifacts().get(0).hashCode());
        assertNotEquals(artifacts.artifacts().get(0), EmittedArtifact.utf8("a/B.java", "A"));
        assertNotEquals(artifacts.artifacts().get(0), EmittedArtifact.utf8("a/A.java", "B"));
        assertNotEquals(artifacts.artifacts().get(0), "A");
        assertEquals(Map.of("a/A.java", "A", "z/B.java", "B"), artifacts.utf8TextByPath());
        assertThrows(UnsupportedOperationException.class,
                () -> artifacts.utf8TextByPath().put("c/C.java", "C"));
        assertEquals(Map.of("a/A.java", "A"),
                EmittedArtifacts.fromUtf8(Map.of("a/A.java", "A")).utf8TextByPath());
        assertTrue(EmittedArtifacts.fromUtf8(null).artifacts().isEmpty());
        assertTrue(EmittedArtifacts.of(null).artifacts().isEmpty());
        assertTrue(new EmittedArtifacts(null).artifacts().isEmpty());
        assertEquals("a/A.java", EmittedArtifact.utf8("a\\A.java", "A").path());
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("../A.java", "A"));
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("", "A"));
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("/A.java", "A"));
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("C:/A.java", "A"));
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("a//A.java", "A"));
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("a/A.java/", "A"));
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("a\\A.java\\", "A"));
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("./A.java", "A"));
        assertThrows(NullPointerException.class, () -> new EmittedArtifact("A.java", null));
        assertThrows(NullPointerException.class, () -> EmittedArtifact.utf8("A.java", null));
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifacts.of(List.of(
                EmittedArtifact.utf8("A.java", "A"), EmittedArtifact.utf8("A.java", "B"))));
    }

    private static SemanticProgram program() {
        SourceSpan span = new SourceSpan("src/program.cob", 1, 1, 1, 9);
        SourceProvenance provenance = new SourceProvenance("src/program.cob", "0".repeat(64),
                "COBOL", Optional.empty(), List.of());
        return new SemanticProgram("1", SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.PROGRAM, "program", span), "TEST", provenance,
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
    }
}
