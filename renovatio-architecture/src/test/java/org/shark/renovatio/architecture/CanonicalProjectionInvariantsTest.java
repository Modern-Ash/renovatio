package org.shark.renovatio.architecture;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.profile.MigrationProfile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class CanonicalProjectionInvariantsTest {
    private static final String HASH = "a".repeat(64);
    private static final String MODULE = "b".repeat(64);
    private static final String COMPONENT = "c".repeat(64);

    @Test void projectionPreservesIdentityOrderingAndProvenance() {
        DomainModel domain = domain(); DecisionSet decisions = decisions(); ArchitectureGraph graph = graph();
        ArchitectureProjector projector = new ArchitectureProjector();
        ArchitectureModel first = projector.project(domain, decisions, HASH, graph);
        ArchitectureModel second = projector.project(domain, decisions, HASH, graph);
        assertEquals(first, second); assertEquals(first.canonicalHash(), second.canonicalHash());
        assertEquals(domain.canonicalHash(), first.domainModelHash());
        assertEquals(decisions.canonicalHash(), first.decisionSetHash());
        assertFalse(first.components().getFirst().evidenceHashes().isEmpty());
        assertEquals(graph, first.legacyGraph());
    }

    @Test void contentChangesAlterCanonicalHash() {
        ArchitectureModel first = new ArchitectureProjector().project(domain(), decisions(), HASH, graph());
        DecisionSet changed = new DecisionSet(DecisionSet.SCHEMA_VERSION, HASH,
                MigrationProfile.ArchitectureStyle.LAYERED, MigrationProfile.Language.JAVA,
                Map.of("PAY001", "payments"), List.of());
        ArchitectureModel second = new ArchitectureProjector().project(domain(), changed, HASH, graph());
        assertNotEquals(first.canonicalHash(), second.canonicalHash());
    }

    @Test void unsupportedSchemasAndDanglingReferencesFailActionably() {
        IllegalArgumentException schema = assertThrows(IllegalArgumentException.class, () ->
                new DecisionSet("99", HASH, MigrationProfile.ArchitectureStyle.HEXAGONAL,
                        MigrationProfile.Language.JAVA, Map.of("PAY001", "payments"), List.of()));
        assertTrue(schema.getMessage().contains("supported"));
        assertThrows(IllegalArgumentException.class, () -> new ArchitectureModel(
                ArchitectureModel.SCHEMA_VERSION, HASH, HASH, HASH,
                List.of(new ArchitectureModel.Module(MODULE, "payments", List.of("PAY001"))),
                List.of(new ArchitectureModel.Component(COMPONENT, "d".repeat(64), "PAY001", null,
                        ArchitectureModel.ComponentKind.SERVICE, "Payment")), List.of()));
    }

    private static DomainModel domain() {
        DomainModel.Evidence evidence = new DomainModel.Evidence("PAY001.cbl", HASH, "program header");
        return new DomainModel(DomainModel.SCHEMA_VERSION, "project", List.of(
                new DomainModel.DomainNode("aggregate:PAY001", DomainModel.Kind.AGGREGATE, "Payment",
                        List.of(evidence), DomainModel.Origin.DETERMINISTIC, 1.0)), List.of(), List.of());
    }
    private static DecisionSet decisions() {
        return new DecisionSet(DecisionSet.SCHEMA_VERSION, HASH,
                MigrationProfile.ArchitectureStyle.HEXAGONAL, MigrationProfile.Language.JAVA,
                Map.of("PAY001", "payments"), List.of("architecture-style"));
    }
    private static ArchitectureGraph graph() {
        return new ArchitectureGraph(
                List.of(new ArchitectureGraph.Module(MODULE, "payments", List.of("PAY001"))),
                List.of(new ArchitectureGraph.Component(COMPONENT, MODULE, "PAY001", Optional.empty(),
                        ArchitectureGraph.ComponentKind.SERVICE, "Payment")), List.of());
    }
}
