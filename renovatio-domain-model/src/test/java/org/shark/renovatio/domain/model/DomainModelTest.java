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
    void propertiesAndCardinalitiesParticipateInCanonicalHash() {
        var evidence = new DomainModel.Evidence("src/customer.cpy:4", "a".repeat(64), "PIC X(12)");
        var withName = new DomainModel.DomainNode("customer", DomainModel.Kind.ENTITY, "Customer",
                List.of(new DomainModel.Property("name", "string", true, List.of(evidence))),
                List.of(evidence), DomainModel.Origin.DETERMINISTIC, 1.0);
        var relation = new DomainModel.DomainRelation("owns", "customer", "account",
                DomainModel.RelationKind.CONTAINS, DomainModel.Cardinality.ONE,
                DomainModel.Cardinality.ONE_OR_MORE);
        var model = new DomainModel("1", "p1",
                List.of(withName, node("account", DomainModel.Kind.AGGREGATE)), List.of(relation), List.of());

        assertEquals(DomainModel.Cardinality.ONE_OR_MORE, model.relations().get(0).targetCardinality());
        assertNotEquals(new DomainModel("1", "p1",
                List.of(node("customer", DomainModel.Kind.ENTITY), node("account", DomainModel.Kind.AGGREGATE)),
                List.of(relation), List.of()).canonicalHash(), model.canonicalHash());
    }

    @Test
    void physicalDataMappingMetadataParticipatesInCanonicalHash() {
        var evidence = new DomainModel.Evidence("src/CUSTCOPY.cpy:4", "COBOL", "CUSTOMER-ID");
        var mapped = new DomainModel.DomainNode("customer", DomainModel.Kind.ENTITY, "Customer",
                List.of(new DomainModel.Property("id", "string", true, List.of(evidence),
                        true, "CUSTOMER_ID", "CUST-ID", "AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS")),
                List.of(evidence), DomainModel.Origin.HUMAN, 1.0,
                "customers", "AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS");
        var account = node("account", DomainModel.Kind.AGGREGATE);
        var relation = new DomainModel.DomainRelation("customer-account", "account", "customer",
                DomainModel.RelationKind.MAPS_TO, DomainModel.Cardinality.ZERO_OR_MORE,
                DomainModel.Cardinality.ONE, new DomainModel.ForeignKey("customerId", "id"));
        var model = new DomainModel("1", "p1", List.of(mapped, account), List.of(relation), List.of());

        assertEquals("customers", model.nodes().stream().filter(node -> node.id().equals("customer"))
                .findFirst().orElseThrow().tableName());
        assertTrue(model.nodes().stream().flatMap(node -> node.properties().stream())
                .anyMatch(property -> property.isKey() && "CUST-ID".equals(property.sourceColumn())));
        assertNotEquals(new DomainModel("1", "p1",
                List.of(node("customer", DomainModel.Kind.ENTITY), account), List.of(relation), List.of()).canonicalHash(),
                model.canonicalHash());
    }

    @Test
    void rejectsDuplicatePropertiesAndInvariantIds() {
        assertThrows(IllegalArgumentException.class, () -> new DomainModel.DomainNode(
                "customer", DomainModel.Kind.ENTITY, "Customer",
                List.of(new DomainModel.Property("name", "string", true, List.of()),
                        new DomainModel.Property("name", "text", false, List.of())),
                List.of(), DomainModel.Origin.HUMAN, 1.0));

        var subject = node("customer", DomainModel.Kind.ENTITY);
        var invariant = new DomainModel.BusinessInvariant("unique-email", subject.id(), "email is unique", subject.evidence());
        assertThrows(IllegalArgumentException.class, () -> new DomainModel("1", "p1", List.of(subject), List.of(),
                List.of(invariant, invariant)));
    }

    @Test
    void canonicalHashNormalizesInvariantEvidenceOrdering() {
        var subject = node("customer", DomainModel.Kind.ENTITY);
        var first = new DomainModel.Evidence("src/a.cpy:1", "COBOL", "first");
        var second = new DomainModel.Evidence("src/b.cpy:2", "COBOL", "second");
        var left = new DomainModel.BusinessInvariant("rule", subject.id(), "must balance",
                List.of(second, first));
        var right = new DomainModel.BusinessInvariant("rule", subject.id(), "must balance",
                List.of(first, second));

        assertEquals(new DomainModel("1", "p1", List.of(subject), List.of(), List.of(left)).canonicalHash(),
                new DomainModel("1", "p1", List.of(subject), List.of(), List.of(right)).canonicalHash());
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
