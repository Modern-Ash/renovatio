package org.shark.renovatio.architecture;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;

/** Canonical architecture model produced by projecting DomainModel + DecisionSet. */
public record ArchitectureModel(String schemaVersion, String requestHash, String domainModelHash,
                                 String decisionSetHash, List<Module> modules,
                                 List<Component> components, List<Relation> relations) {
    public static final String SCHEMA_VERSION = "1";

    public ArchitectureModel {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("unsupported schemaVersion");
        requestHash = hash(requestHash, "requestHash");
        domainModelHash = hash(domainModelHash, "domainModelHash");
        decisionSetHash = hash(decisionSetHash, "decisionSetHash");
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

    public String canonicalHash() {
        String canonical = schemaVersion + "\n" + requestHash + "\n" + domainModelHash + "\n"
                + decisionSetHash + "\n" + modules + "\n" + components + "\n" + relations;
        return ArchitectureSupport.sha256(canonical);
    }

    public ArchitectureGraph legacyGraph() {
        return new ArchitectureGraph(
                modules.stream().map(value -> new ArchitectureGraph.Module(value.id(), value.name(), value.programIds())).toList(),
                components.stream().map(value -> new ArchitectureGraph.Component(value.id(), value.moduleId(),
                        value.programId(), Optional.ofNullable(value.semanticNodeId()),
                        ArchitectureGraph.ComponentKind.valueOf(value.kind().name()), value.name())).toList(),
                relations.stream().map(value -> new ArchitectureGraph.Relation(value.id(), value.fromComponentId(),
                        value.toComponentId(), ArchitectureGraph.RelationKind.valueOf(value.kind().name()))).toList());
    }

    public enum ComponentKind { SERVICE, USE_CASE, INBOUND_PORT, OUTBOUND_PORT, ADAPTER, ENTITY, VALUE, UNRESOLVED }
    public enum RelationKind { INVOKES, IMPLEMENTS, USES, READS, WRITES, UNKNOWN }

    public record Module(String id, String name, List<String> programIds) {
        public Module {
            id = hash(id, "moduleId");
            name = moduleName(name);
            programIds = (programIds == null ? List.<String>of() : programIds).stream()
                    .map(ArchitectureModel::normalizeProgramId).distinct().sorted().toList();
            if (programIds.isEmpty()) throw new IllegalArgumentException("module programs must not be empty");
        }
    }

    public record Component(String id, String moduleId, String programId, String semanticNodeId,
                            ComponentKind kind, String name, List<String> evidenceHashes) {
        public Component {
            id = hash(id, "componentId");
            moduleId = hash(moduleId, "moduleId");
            programId = normalizeProgramId(programId);
            semanticNodeId = semanticNodeId == null ? null : hash(semanticNodeId, "semanticNodeId");
            Objects.requireNonNull(kind, "kind");
            name = text(name, "componentName");
            evidenceHashes = (evidenceHashes == null ? List.<String>of() : evidenceHashes).stream()
                    .map(h -> hash(h, "evidenceHash")).distinct().sorted().toList();
        }

        public Component(String id, String moduleId, String programId, String semanticNodeId,
                         ComponentKind kind, String name) {
            this(id, moduleId, programId, semanticNodeId, kind, name, List.of());
        }
    }

    public record Relation(String id, String fromComponentId, String toComponentId, RelationKind kind) {
        public Relation {
            id = hash(id, "relationId");
            fromComponentId = hash(fromComponentId, "fromComponentId");
            toComponentId = hash(toComponentId, "toComponentId");
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

    static String hash(String value, String name) {
        return ArchitectureSupport.hash(value, name);
    }

    static String normalizeProgramId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("programId is required");
        return value.strip();
    }

    static String text(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }

    static String moduleName(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("moduleName is required");
        return value.strip().toLowerCase().replaceAll("[^a-z0-9._-]", "-");
    }
}
