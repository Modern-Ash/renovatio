package org.shark.renovatio.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

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

    private static DomainModel.DomainNode node(String id, DomainModel.Kind kind) {
        return new DomainModel.DomainNode(id, kind, id, List.of(
                new DomainModel.Evidence("src/" + id + ".cob:1", "COBOL", "fixture")),
                DomainModel.Origin.DETERMINISTIC, 1.0);
    }
}
