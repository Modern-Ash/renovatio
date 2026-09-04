package org.shark.renovatio.architecture;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainArchitectureProjectorTest {
    private static DomainModel model() {
        return new DomainModel("1", "demo", List.of(
                new DomainModel.DomainNode("order", DomainModel.Kind.AGGREGATE, "Order", List.of(), DomainModel.Origin.HUMAN, 1),
                new DomainModel.DomainNode("place", DomainModel.Kind.USE_CASE, "Place order", List.of(), DomainModel.Origin.HUMAN, 1),
                new DomainModel.DomainNode("orders", DomainModel.Kind.REPOSITORY, "Orders", List.of(), DomainModel.Origin.DETERMINISTIC, 1),
                new DomainModel.DomainNode("payments", DomainModel.Kind.EXTERNAL_SYSTEM, "Payments", List.of(), DomainModel.Origin.LLM, .8)),
                List.of(new DomainModel.DomainRelation("uses", "place", "orders", DomainModel.RelationKind.USES)), List.of());
    }

    @Test void projectsStylesAndIsDeterministic() {
        var a = DomainArchitectureProjector.project(model(), MigrationProfile.ArchitectureStyle.HEXAGONAL);
        var b = DomainArchitectureProjector.project(model(), MigrationProfile.ArchitectureStyle.HEXAGONAL);
        assertEquals(a, b);
        assertEquals(ArchitectureModel.Kind.PORT, a.components().stream().filter(c -> c.id().equals("orders")).findFirst().orElseThrow().kind());
        assertEquals(ArchitectureModel.Kind.ADAPTER, DomainArchitectureProjector.project(model(), MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT)
                .components().stream().filter(c -> c.id().equals("payments")).findFirst().orElseThrow().kind());
        assertEquals(ArchitectureModel.Kind.CONTROLLER, DomainArchitectureProjector.project(model(), MigrationProfile.ArchitectureStyle.LAYERED_MVC)
                .components().stream().filter(c -> c.id().equals("place")).findFirst().orElseThrow().kind());
    }
}
