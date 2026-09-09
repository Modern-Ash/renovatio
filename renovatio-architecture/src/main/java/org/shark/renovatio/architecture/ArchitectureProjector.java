package org.shark.renovatio.architecture;

import org.shark.renovatio.domain.model.DomainModel;
import java.util.List;
import java.util.Objects;

/** Canonical target-neutral projection from domain and decisions over the resolved graph. */
public final class ArchitectureProjector {
    public ArchitectureModel project(DomainModel domain, DecisionSet decisions, String requestHash,
                                     ArchitectureGraph resolved) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(decisions, "decisions");
        Objects.requireNonNull(resolved, "resolved");
        List<String> evidence = domain.nodes().stream().flatMap(node -> node.evidence().stream())
                .map(DomainModel.Evidence::provenance).map(ArchitectureSupport::sha256).distinct().sorted().toList();
        return new ArchitectureModel(ArchitectureModel.SCHEMA_VERSION, requestHash, domain.canonicalHash(),
                decisions.canonicalHash(),
                resolved.modules().stream().map(value -> new ArchitectureModel.Module(
                        value.id(), value.name(), value.programIds())).toList(),
                resolved.components().stream().map(value -> new ArchitectureModel.Component(value.id(),
                        value.moduleId(), value.programId(), value.semanticNodeId().orElse(null),
                        ArchitectureModel.ComponentKind.valueOf(value.kind().name()), value.name(), evidence)).toList(),
                resolved.relations().stream().map(value -> new ArchitectureModel.Relation(value.id(),
                        value.fromComponentId(), value.toComponentId(),
                        ArchitectureModel.RelationKind.valueOf(value.kind().name()))).toList());
    }
}
