package org.shark.renovatio.architecture;

import org.shark.renovatio.domain.model.DomainModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Sole authority for projecting domain facts and architectural decisions. */
public final class ArchitectureProjector {
    public ArchitectureModel project(DomainModel domain, DecisionSet decisions, String requestHash) {
        Objects.requireNonNull(domain, "domain"); Objects.requireNonNull(decisions, "decisions");
        Map<String, String> moduleIds = new TreeMap<>();
        decisions.moduleByProgram().values().stream().distinct().sorted().forEach(name ->
                moduleIds.put(name, ArchitectureSupport.id(requestHash, "MODULE", name, name, "module")));
        List<ArchitectureModel.Module> modules = moduleIds.entrySet().stream().map(entry ->
                new ArchitectureModel.Module(entry.getValue(), entry.getKey(), decisions.moduleByProgram().entrySet()
                        .stream().filter(value -> value.getValue().equals(entry.getKey())).map(Map.Entry::getKey).toList())).toList();
        List<String> evidence = domain.nodes().stream().flatMap(node -> node.evidence().stream())
                .map(DomainModel.Evidence::provenance).map(ArchitectureSupport::sha256).distinct().sorted().toList();
        List<ArchitectureModel.Component> components = new ArrayList<>();
        List<ArchitectureModel.Relation> syntheticRelations = new ArrayList<>();
        Map<String, String> primaryByDomainNode = new TreeMap<>();
        for (DomainModel.DomainNode node : domain.nodes()) {
            if (isLayered(decisions.architectureStyle()) && node.kind() == DomainModel.Kind.USE_CASE) continue;
            String program = programId(node.id());
            String moduleName = decisions.moduleByProgram().get(program);
            if (moduleName == null) throw new IllegalArgumentException("domain node references unknown program " + program);
            ArchitectureModel.ComponentKind kind = primaryKind(node, decisions.architectureStyle());
            String name = componentName(node, decisions.architectureStyle(), kind);
            ArchitectureModel.Component primary = component(requestHash, moduleIds.get(moduleName), program,
                    node.id(), kind, name, evidence);
            components.add(primary);
            primaryByDomainNode.put(node.id(), primary.id());
            if (isLayered(decisions.architectureStyle()) && node.kind() == DomainModel.Kind.AGGREGATE) {
                ArchitectureModel.Component controller = component(requestHash, moduleIds.get(moduleName), program,
                        node.id(), ArchitectureModel.ComponentKind.ADAPTER, node.name() + " controller", evidence);
                ArchitectureModel.Component model = component(requestHash, moduleIds.get(moduleName), program,
                        node.id(), ArchitectureModel.ComponentKind.ENTITY, node.name() + " model", evidence);
                components.add(controller); components.add(model);
                syntheticRelations.add(relation(requestHash, controller.id(), primary.id(),
                        ArchitectureModel.RelationKind.INVOKES, node.id() + ":controller-service"));
                syntheticRelations.add(relation(requestHash, primary.id(), model.id(),
                        ArchitectureModel.RelationKind.USES, node.id() + ":service-model"));
            }
            if (decisions.architectureStyle() == org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle.HEXAGONAL
                    && node.kind() == DomainModel.Kind.AGGREGATE) {
                ArchitectureModel.Component inbound = component(requestHash, moduleIds.get(moduleName), program,
                        node.id(), ArchitectureModel.ComponentKind.INBOUND_PORT, node.name() + " inbound port", evidence);
                components.add(inbound);
                syntheticRelations.add(relation(requestHash, inbound.id(), primary.id(),
                        ArchitectureModel.RelationKind.INVOKES, node.id() + ":inbound-use-case"));
            }
            if (decisions.architectureStyle() == org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle.HEXAGONAL
                    && (node.kind() == DomainModel.Kind.REPOSITORY || node.kind() == DomainModel.Kind.EXTERNAL_SYSTEM)
                    && kind != ArchitectureModel.ComponentKind.UNRESOLVED) {
                components.add(component(requestHash, moduleIds.get(moduleName), program, node.id(),
                        ArchitectureModel.ComponentKind.ADAPTER, node.name() + " adapter", evidence));
            }
            if (decisions.architectureStyle() == org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle.HEXAGONAL
                    && node.kind() == DomainModel.Kind.USE_CASE) {
                components.add(component(requestHash, moduleIds.get(moduleName), program, node.id(),
                        ArchitectureModel.ComponentKind.INBOUND_PORT, node.name() + " port", evidence));
            }
        }
        Map<String, DomainModel.DomainNode> nodes = domain.nodes().stream().collect(
                java.util.stream.Collectors.toMap(DomainModel.DomainNode::id, value -> value));
        List<ArchitectureModel.Relation> relations = new ArrayList<>(syntheticRelations);
        relations.addAll(domain.relations().stream()
                .filter(value -> primaryByDomainNode.containsKey(value.fromId())
                        && primaryByDomainNode.containsKey(value.toId())).map(relation -> {
            DomainModel.DomainNode target = nodes.get(relation.toId());
            ArchitectureModel.RelationKind kind = target != null && target.kind() == DomainModel.Kind.REPOSITORY
                    ? ArchitectureModel.RelationKind.READS : switch (relation.kind()) {
                case IMPLEMENTS, MAPS_TO, CONTAINS -> ArchitectureModel.RelationKind.IMPLEMENTS;
                case PUBLISHES -> ArchitectureModel.RelationKind.WRITES;
                case SUBSCRIBES_TO -> ArchitectureModel.RelationKind.READS;
                default -> ArchitectureModel.RelationKind.USES;
            };
            return new ArchitectureModel.Relation(ArchitectureSupport.id(requestHash, "RELATION",
                    primaryByDomainNode.get(relation.fromId()), programId(relation.fromId()), relation.id()),
                    primaryByDomainNode.get(relation.fromId()), primaryByDomainNode.get(relation.toId()), kind);
        }).toList());
        return new ArchitectureModel(ArchitectureModel.SCHEMA_VERSION, requestHash, domain.canonicalHash(),
                decisions.canonicalHash(), modules, components, relations);
    }

    private static ArchitectureModel.Component component(String requestHash, String moduleId, String program,
            String semanticId, ArchitectureModel.ComponentKind kind, String name, List<String> evidence) {
        return new ArchitectureModel.Component(ArchitectureSupport.id(requestHash, "COMPONENT", moduleId,
                program, semanticId + ":" + kind), moduleId, program, ArchitectureSupport.sha256(semanticId),
                kind, name, evidence);
    }
    private static ArchitectureModel.Relation relation(String requestHash, String from, String to,
            ArchitectureModel.RelationKind kind, String discriminator) {
        return new ArchitectureModel.Relation(ArchitectureSupport.id(requestHash, "RELATION", from,
                to, discriminator), from, to, kind);
    }
    private static String programId(String id) {
        String[] parts = id.split(":", 3);
        if (parts.length < 2 || parts[1].isBlank()) throw new IllegalArgumentException("domain node has no program identity: " + id);
        return parts[1];
    }
    private static ArchitectureModel.ComponentKind primaryKind(DomainModel.DomainNode node,
            org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle style) {
        if (node.id().startsWith("unresolved:")) return ArchitectureModel.ComponentKind.UNRESOLVED;
        if (node.kind() == DomainModel.Kind.AGGREGATE
                && style == org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle.HEXAGONAL) {
            return ArchitectureModel.ComponentKind.USE_CASE;
        }
        return switch (node.kind()) {
            case AGGREGATE, DOMAIN_SERVICE, BOUNDED_CONTEXT, EVENT -> ArchitectureModel.ComponentKind.SERVICE;
            case USE_CASE -> ArchitectureModel.ComponentKind.USE_CASE;
            case ENTITY -> ArchitectureModel.ComponentKind.ENTITY;
            case VALUE_OBJECT -> ArchitectureModel.ComponentKind.VALUE;
            case REPOSITORY, EXTERNAL_SYSTEM -> ArchitectureModel.ComponentKind.OUTBOUND_PORT;
        };
    }
    private static boolean isLayered(org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle style) {
        return style == org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle.LAYERED
                || style == org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle.LAYERED_MVC
                || style == org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle.CLEAN;
    }
    private static String componentName(DomainModel.DomainNode node,
            org.shark.renovatio.profile.MigrationProfile.ArchitectureStyle style,
            ArchitectureModel.ComponentKind kind) {
        if (isLayered(style)) {
            return node.name() + switch (kind) { case USE_CASE -> " controller"; case ENTITY, VALUE -> " model"; default -> " service"; };
        }
        return node.name();
    }
}
