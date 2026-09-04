package org.shark.renovatio.architecture;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.profile.MigrationProfile;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureModelAdapterTest {
    @Test void adaptsProjectionToExistingGraph() {
        var domain = new DomainModel("1", "demo", List.of(
                new DomainModel.DomainNode("use", DomainModel.Kind.USE_CASE, "Use", List.of(), DomainModel.Origin.HUMAN, 1)), List.of(), List.of());
        var projection = DomainArchitectureProjector.project(domain, MigrationProfile.ArchitectureStyle.LAYERED_MVC);
        var graph = ArchitectureModelAdapter.toGraph(projection);
        assertEquals(1, graph.modules().size());
        assertEquals(ArchitectureGraph.ComponentKind.USE_CASE, graph.components().get(0).kind());
    }
}
