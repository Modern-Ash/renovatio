package org.shark.renovatio.architecture;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Target-neutral module/component/relation graph rendered by adapters. */
public record ArchitectureGraph(List<Module> modules, List<Component> components, List<Relation> relations) {
    public ArchitectureGraph {
        modules = sorted(modules, Module::id);
        components = sorted(components, Component::id);
        relations = sorted(relations, Relation::id);
        unique(modules.stream().map(Module::id).toList(), "module id");
        unique(components.stream().map(Component::id).toList(), "component id");
        unique(relations.stream().map(Relation::id).toList(), "relation id");
        Set<String> moduleIds = new HashSet<>(modules.stream().map(Module::id).toList());
        Set<String> componentIds = new HashSet<>(components.stream().map(Component::id).toList());
        components.forEach(value -> {
            if (!moduleIds.contains(value.moduleId())) throw new IllegalArgumentException("dangling component module");
        });
        relations.forEach(value -> {
            if (!componentIds.contains(value.fromComponentId()) || !componentIds.contains(value.toComponentId())) {
                throw new IllegalArgumentException("dangling relation component");
            }
        });
    }

    public enum ComponentKind { SERVICE, USE_CASE, INBOUND_PORT, OUTBOUND_PORT, ADAPTER, ENTITY, VALUE, UNRESOLVED }
    public enum RelationKind { INVOKES, IMPLEMENTS, USES, READS, WRITES, UNKNOWN }

    public record Module(String id, String name, List<String> programIds) {
        public Module {
            id = ArchitectureSupport.hash(id, "moduleId");
            name = ArchitectureSupport.moduleName(name);
            programIds = (programIds == null ? List.<String>of() : programIds).stream()
                    .map(ArchitectureSupport::program).distinct().sorted().toList();
            if (programIds.isEmpty()) throw new IllegalArgumentException("module programs must not be empty");
        }
    }

    public record Component(String id, String moduleId, String programId, Optional<String> semanticNodeId,
                            ComponentKind kind, String name) {
        public Component {
            id = ArchitectureSupport.hash(id, "componentId");
            moduleId = ArchitectureSupport.hash(moduleId, "moduleId");
            programId = ArchitectureSupport.program(programId);
            semanticNodeId = semanticNodeId == null ? Optional.empty()
                    : semanticNodeId.map(value -> ArchitectureSupport.hash(value, "semanticNodeId"));
            Objects.requireNonNull(kind, "kind");
            name = ArchitectureSupport.text(name, "componentName");
        }
    }

    public record Relation(String id, String fromComponentId, String toComponentId, RelationKind kind) {
        public Relation {
            id = ArchitectureSupport.hash(id, "relationId");
            fromComponentId = ArchitectureSupport.hash(fromComponentId, "fromComponentId");
            toComponentId = ArchitectureSupport.hash(toComponentId, "toComponentId");
            Objects.requireNonNull(kind, "kind");
        }
    }

    private static <T> List<T> sorted(List<T> values, java.util.function.Function<T, String> id) {
        return (values == null ? List.<T>of() : values).stream().peek(Objects::requireNonNull)
                .sorted(java.util.Comparator.comparing(id)).toList();
    }

    private static void unique(List<String> values, String name) {
        if (new HashSet<>(values).size() != values.size()) throw new IllegalArgumentException("duplicate " + name);
    }
}
