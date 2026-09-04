package org.shark.renovatio.architecture;

import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.ArrayList;
import java.util.List;

/** Deterministically maps the approved business model into a selectable architecture style. */
public final class DomainArchitectureProjector {
    private DomainArchitectureProjector() { }

    public static ArchitectureModel project(DomainModel model, MigrationProfile.ArchitectureStyle style) {
        if (model == null || style == null) throw new IllegalArgumentException("model and style are required");
        List<ArchitectureModel.Component> components = new ArrayList<>();
        for (DomainModel.DomainNode node : model.nodes()) {
            ArchitectureModel.Kind kind = kind(node.kind(), style);
            components.add(new ArchitectureModel.Component(node.id(), kind, node.name(), node.id()));
        }
        List<ArchitectureModel.Relation> relations = model.relations().stream()
                .map(r -> new ArchitectureModel.Relation(r.id(), r.fromId(), r.toId(), r.kind().name()))
                .toList();
        return new ArchitectureModel(ArchitectureModel.SCHEMA_VERSION, model.canonicalHash(), style, style,
                components, relations, List.of("Projection is derived only from DomainModel; COBOL is not reinterpreted."));
    }

    private static ArchitectureModel.Kind kind(DomainModel.Kind source, MigrationProfile.ArchitectureStyle style) {
        return switch (style) {
            case TRANSACTION_SCRIPT -> switch (source) {
                case USE_CASE, DOMAIN_SERVICE -> ArchitectureModel.Kind.SERVICE;
                case REPOSITORY -> ArchitectureModel.Kind.REPOSITORY;
                case EXTERNAL_SYSTEM -> ArchitectureModel.Kind.ADAPTER;
                default -> ArchitectureModel.Kind.DOMAIN;
            };
            case LAYERED_MVC -> switch (source) {
                case USE_CASE -> ArchitectureModel.Kind.CONTROLLER;
                case DOMAIN_SERVICE -> ArchitectureModel.Kind.SERVICE;
                case REPOSITORY -> ArchitectureModel.Kind.REPOSITORY;
                case EXTERNAL_SYSTEM -> ArchitectureModel.Kind.ADAPTER;
                default -> ArchitectureModel.Kind.DOMAIN;
            };
            case HEXAGONAL -> switch (source) {
                case USE_CASE, DOMAIN_SERVICE -> ArchitectureModel.Kind.SERVICE;
                case REPOSITORY -> ArchitectureModel.Kind.PORT;
                case EXTERNAL_SYSTEM -> ArchitectureModel.Kind.ADAPTER;
                default -> ArchitectureModel.Kind.DOMAIN;
            };
        };
    }
}
