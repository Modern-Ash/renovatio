package org.shark.renovatio.shared.emission;

import org.junit.jupiter.api.Test;
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
        assertThrows(IllegalArgumentException.class, () -> EmittedArtifact.utf8("../A.java", "A"));
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
