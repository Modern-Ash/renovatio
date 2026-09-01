package org.shark.renovatio.architecture;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureContractsTest {
    private static final String A = "a".repeat(64);
    private static final String B = "b".repeat(64);
    private static final String C = "c".repeat(64);

    @Test
    void graphAndManifestAreCanonicalAndRejectDanglingOrAliasingValues() {
        var module = new ArchitectureGraph.Module(A, "Payments", List.of("PAY001"));
        var service = new ArchitectureGraph.Component(B, A, "PAY001", Optional.empty(),
                ArchitectureGraph.ComponentKind.SERVICE, "Payment service");
        ArchitectureGraph graph = new ArchitectureGraph(List.of(module), List.of(service), List.of());
        assertEquals("payments", graph.modules().get(0).name());
        assertThrows(UnsupportedOperationException.class, () -> graph.components().add(service));
        assertThrows(IllegalArgumentException.class, () -> new ArchitectureGraph(List.of(module),
                List.of(new ArchitectureGraph.Component(B, C, "PAY001", Optional.empty(),
                        ArchitectureGraph.ComponentKind.SERVICE, "Bad")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ArchitectureGraph(List.of(module), List.of(service),
                List.of(new ArchitectureGraph.Relation(C, B, A, ArchitectureGraph.RelationKind.USES))));

        var artifact = new ArtifactManifest.Artifact(C, "generated/Pay.java", B, A, "PAY001",
                MigrationProfile.Language.JAVA, "service");
        ArtifactManifest manifest = new ArtifactManifest(List.of(artifact));
        assertEquals("generated/Pay.java", manifest.artifacts().get(0).path());
        assertThrows(IllegalArgumentException.class, () -> new ArtifactManifest(List.of(artifact,
                new ArtifactManifest.Artifact("d".repeat(64), "generated/Pay.java", B, A, "PAY001",
                        MigrationProfile.Language.JAVA, "duplicate"))));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactManifest.Artifact(C,
                "generated/Pay.java/", B, A, "PAY001", MigrationProfile.Language.JAVA, "bad"));
    }
}
