package org.shark.renovatio.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {
    @Test
    void canonicalHashIsIndependentOfInputOrdering() {
        var a = new DomainModel("1", "p1", List.of(
                node("b", DomainModel.Kind.USE_CASE), node("a", DomainModel.Kind.ENTITY)),
                List.of(new DomainModel.DomainRelation("r", "a", "b", DomainModel.RelationKind.USES)), List.of());
        var b = new DomainModel("1", "p1", List.of(
                node("a", DomainModel.Kind.ENTITY), node("b", DomainModel.Kind.USE_CASE)),
                List.of(new DomainModel.DomainRelation("r", "a", "b", DomainModel.RelationKind.USES)), List.of());
        assertEquals(a.canonicalHash(), b.canonicalHash());
    }

    @Test
    void rejectsRelationsWithoutEvidenceNodes() {
        assertThrows(IllegalArgumentException.class, () -> new DomainModel("1", "p1",
                List.of(node("a", DomainModel.Kind.ENTITY)),
                List.of(new DomainModel.DomainRelation("r", "a", "missing", DomainModel.RelationKind.USES)), List.of()));
    }

    @Test
    void projectsProgramIntoAggregateAndUseCaseWithSourceEvidence() {
        SourceSpan span = new SourceSpan("src/demo.cob", 1, 1, 1, 8);
        SourceProvenance provenance = new SourceProvenance("src/demo.cob", "a".repeat(64), "COBOL", Optional.empty(), List.of());
        SemanticProgram program = new SemanticProgram("1", SemanticProgram.Header.create("DEMO",
                SemanticProgram.NodeKind.PROGRAM, "program", span), "DEMO", provenance,
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
        DomainModel model = new SemanticDomainProjector().project("project-1", List.of(program));
        assertEquals(2, model.nodes().size());
        assertTrue(model.nodes().stream().allMatch(node -> !node.evidence().isEmpty()));
        assertEquals(1, model.relations().size());
    }

    private static DomainModel.DomainNode node(String id, DomainModel.Kind kind) {
        return new DomainModel.DomainNode(id, kind, id, List.of(
                new DomainModel.Evidence("src/" + id + ".cob:1", "COBOL", "fixture")),
                DomainModel.Origin.DETERMINISTIC, 1.0);
    }
}
