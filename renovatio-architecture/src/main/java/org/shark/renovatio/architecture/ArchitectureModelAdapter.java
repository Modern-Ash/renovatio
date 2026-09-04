package org.shark.renovatio.architecture;

import java.util.List;
import java.util.Optional;

/** Bridges the DomainModel projection into the existing preview graph contract. */
public final class ArchitectureModelAdapter {
    private ArchitectureModelAdapter() { }

    public static ArchitectureGraph toGraph(ArchitectureModel model) {
        if (model == null) throw new IllegalArgumentException("model is required");
        List<ArchitectureGraph.Module> modules = model.components().stream()
                .map(c -> new ArchitectureGraph.Module(hash(c.id()), c.name(), List.of(c.domainNodeId().isBlank() ? c.id() : c.domainNodeId())))
                .toList();
        List<ArchitectureGraph.Component> components = model.components().stream()
                .map(c -> new ArchitectureGraph.Component(hash(c.id()), hash(c.id()), c.domainNodeId().isBlank() ? c.id() : c.domainNodeId(),
                        c.domainNodeId().isBlank() ? Optional.empty() : Optional.of(hash(c.domainNodeId())), mapKind(c.kind()), c.name()))
                .toList();
        List<ArchitectureGraph.Relation> relations = model.relations().stream()
                .map(r -> new ArchitectureGraph.Relation(hash(r.id()), hash(r.fromId()), hash(r.toId()), ArchitectureGraph.RelationKind.USES))
                .toList();
        return new ArchitectureGraph(modules, components, relations);
    }

    private static String hash(String value) { return ArchitectureSupport.sha256("domain-architecture\n" + value); }

    private static ArchitectureGraph.ComponentKind mapKind(ArchitectureModel.Kind kind) {
        return switch (kind) {
            case SERVICE -> ArchitectureGraph.ComponentKind.SERVICE;
            case CONTROLLER -> ArchitectureGraph.ComponentKind.USE_CASE;
            case REPOSITORY -> ArchitectureGraph.ComponentKind.OUTBOUND_PORT;
            case PORT -> ArchitectureGraph.ComponentKind.OUTBOUND_PORT;
            case ADAPTER -> ArchitectureGraph.ComponentKind.ADAPTER;
            case DOMAIN -> ArchitectureGraph.ComponentKind.ENTITY;
            case MODULE -> ArchitectureGraph.ComponentKind.UNRESOLVED;
        };
    }
}
