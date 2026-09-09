package org.shark.renovatio.architecture;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.profile.MigrationProfile;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CanonicalProjectionInvariantsTest {
    private static final String HASH = "a".repeat(64);
    private static final String MODULE = "b".repeat(64);
    private static final String COMPONENT = "c".repeat(64);

    @Test void projectionPreservesIdentityOrderingAndProvenance() {
        DomainModel domain = domain(); DecisionSet decisions = decisions();
        ArchitectureProjector projector = new ArchitectureProjector();
        ArchitectureModel first = projector.project(domain, decisions, HASH);
        ArchitectureModel second = projector.project(domain, decisions, HASH);
        assertEquals(first, second); assertEquals(first.canonicalHash(), second.canonicalHash());
        assertEquals(domain.canonicalHash(), first.domainModelHash());
        assertEquals(decisions.canonicalHash(), first.decisionSetHash());
        assertFalse(first.components().getFirst().evidenceHashes().isEmpty());
        assertEquals(first.legacyGraph(), second.legacyGraph());
    }

    @Test void contentChangesAlterCanonicalHash() {
        ArchitectureModel first = new ArchitectureProjector().project(domain(), decisions(), HASH);
        DecisionSet changed = new DecisionSet(DecisionSet.SCHEMA_VERSION, HASH,
                MigrationProfile.ArchitectureStyle.LAYERED, MigrationProfile.Language.JAVA,
                Map.of("PAY001", "payments"), List.of());
        ArchitectureModel second = new ArchitectureProjector().project(domain(), changed, HASH);
        assertNotEquals(first.canonicalHash(), second.canonicalHash());
        assertNotEquals(first.components(), second.components());
    }

    @Test void decisionHashAndProjectionAreStableAcrossMapInsertionOrder() {
        Map<String, String> forward = new java.util.LinkedHashMap<>();
        forward.put("PAY002", "payments"); forward.put("PAY001", "core");
        Map<String, String> reverse = new java.util.LinkedHashMap<>();
        reverse.put("PAY001", "core"); reverse.put("PAY002", "payments");
        DecisionSet first = new DecisionSet(DecisionSet.SCHEMA_VERSION, HASH,
                MigrationProfile.ArchitectureStyle.HEXAGONAL, MigrationProfile.Language.JAVA,
                forward, List.of("architecture-style"));
        DecisionSet second = new DecisionSet(DecisionSet.SCHEMA_VERSION, HASH,
                MigrationProfile.ArchitectureStyle.HEXAGONAL, MigrationProfile.Language.JAVA,
                reverse, List.of("architecture-style"));
        DomainModel multiProgram = new DomainModel(DomainModel.SCHEMA_VERSION, "project", List.of(
                node("aggregate:PAY002", "Second"), node("aggregate:PAY001", "First")), List.of(), List.of());
        assertEquals(first.canonicalHash(), second.canonicalHash());
        assertEquals(new ArchitectureProjector().project(multiProgram, first, HASH),
                new ArchitectureProjector().project(multiProgram, second, HASH));
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
        return new DomainModel(DomainModel.SCHEMA_VERSION, "project", List.of(node("aggregate:PAY001", "Payment")),
                List.of(), List.of());
    }
    private static DecisionSet decisions() {
        return new DecisionSet(DecisionSet.SCHEMA_VERSION, HASH,
                MigrationProfile.ArchitectureStyle.HEXAGONAL, MigrationProfile.Language.JAVA,
                Map.of("PAY001", "payments"), List.of("architecture-style"));
    }
    private static DomainModel.DomainNode node(String id, String name) {
        return new DomainModel.DomainNode(id, DomainModel.Kind.AGGREGATE, name,
                List.of(new DomainModel.Evidence(name + ".cbl", HASH, "program header")),
                DomainModel.Origin.DETERMINISTIC, 1.0);
    }
}
