package org.shark.renovatio.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HexFormat;

/** Immutable v1 business model independent of target architecture and language. */
public record DomainModel(String schemaVersion, String projectId, List<DomainNode> nodes,
                          List<DomainRelation> relations, List<BusinessInvariant> invariants) {
    public static final String SCHEMA_VERSION = "1";

    public DomainModel {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("unsupported schemaVersion");
        projectId = text(projectId, "projectId");
        nodes = Objects.requireNonNull(nodes, "nodes").stream().sorted(Comparator.comparing(DomainNode::id)).toList();
        relations = Objects.requireNonNull(relations, "relations").stream().sorted(Comparator.comparing(DomainRelation::id)).toList();
        invariants = Objects.requireNonNull(invariants, "invariants").stream().sorted(Comparator.comparing(BusinessInvariant::id)).toList();
        Set<String> ids = uniqueIds(nodes.stream().map(DomainNode::id).toList(), "domain node");
        uniqueIds(relations.stream().map(DomainRelation::id).toList(), "domain relation");
        uniqueIds(invariants.stream().map(BusinessInvariant::id).toList(), "business invariant");
        relations.forEach(value -> {
            if (!ids.contains(value.fromId()) || !ids.contains(value.toId()))
                throw new IllegalArgumentException("relation references unknown node");
        });
        invariants.forEach(value -> {
            if (!ids.contains(value.subjectId())) throw new IllegalArgumentException("invariant references unknown node");
        });
    }

    private static Set<String> uniqueIds(List<String> values, String label) {
        Set<String> ids = new HashSet<>(values);
        if (ids.size() != values.size()) throw new IllegalArgumentException("duplicate " + label + " id");
        return Set.copyOf(ids);
    }

    public String canonicalHash() {
        String canonical = schemaVersion + "\n" + projectId + "\n"
                + nodes + "\n" + relations + "\n" + invariants;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    public record DomainNode(String id, Kind kind, String name, List<Property> properties,
                             List<Evidence> evidence, Origin origin, double confidence) {
        public DomainNode {
            id = text(id, "id"); name = text(name, "name"); Objects.requireNonNull(kind, "kind");
            properties = properties == null ? List.of() : properties.stream()
                    .sorted(Comparator.comparing(Property::name)).toList();
            Set<String> propertyNames = properties.stream().map(Property::name)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (propertyNames.size() != properties.size()) throw new IllegalArgumentException("duplicate property name");
            evidence = evidence == null ? List.of() : evidence.stream().sorted(Comparator.comparing(Evidence::sourceRef)).toList();
            Objects.requireNonNull(origin, "origin");
            if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1)
                throw new IllegalArgumentException("confidence must be between 0 and 1");
        }

        public DomainNode(String id, Kind kind, String name, List<Evidence> evidence,
                          Origin origin, double confidence) {
            this(id, kind, name, List.of(), evidence, origin, confidence);
        }
    }

    public record Property(String name, String type, boolean required, List<Evidence> evidence) {
        public Property {
            name = text(name, "property name");
            type = text(type, "property type");
            evidence = evidence == null ? List.of() : evidence.stream()
                    .sorted(Comparator.comparing(Evidence::sourceRef)).toList();
        }
    }

    public record DomainRelation(String id, String fromId, String toId, RelationKind kind,
                                 Cardinality sourceCardinality, Cardinality targetCardinality) {
        public DomainRelation {
            id = text(id, "id"); fromId = text(fromId, "fromId"); toId = text(toId, "toId");
            Objects.requireNonNull(kind, "kind");
            sourceCardinality = sourceCardinality == null ? Cardinality.ONE : sourceCardinality;
            targetCardinality = targetCardinality == null ? Cardinality.ONE : targetCardinality;
        }

        public DomainRelation(String id, String fromId, String toId, RelationKind kind) {
            this(id, fromId, toId, kind, Cardinality.ONE, Cardinality.ONE);
        }
    }

    public record BusinessInvariant(String id, String subjectId, String expression, List<Evidence> evidence,
                                    Origin origin, double confidence) {
        public BusinessInvariant {
            id = text(id, "id"); subjectId = text(subjectId, "subjectId"); expression = text(expression, "expression");
            evidence = evidence == null ? List.of() : evidence.stream()
                    .sorted(Comparator.comparing(Evidence::sourceRef)).toList();
            origin = origin == null ? Origin.DETERMINISTIC : origin;
            if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1)
                throw new IllegalArgumentException("confidence must be between 0 and 1");
        }

        public BusinessInvariant(String id, String subjectId, String expression, List<Evidence> evidence) {
            this(id, subjectId, expression, evidence, Origin.DETERMINISTIC, 1.0);
        }
    }

    public record Evidence(String sourceRef, String provenance, String rationale) {
        public Evidence {
            sourceRef = text(sourceRef, "sourceRef"); provenance = text(provenance, "provenance");
            rationale = rationale == null ? "" : rationale;
        }
    }

    public enum Origin { DETERMINISTIC, LLM, HUMAN }
    public enum Kind { ENTITY, VALUE_OBJECT, AGGREGATE, USE_CASE, DOMAIN_SERVICE, REPOSITORY,
        EXTERNAL_SYSTEM, EVENT, BOUNDED_CONTEXT }
    public enum Cardinality { ONE, ZERO_OR_ONE, ONE_OR_MORE, ZERO_OR_MORE }
    public enum RelationKind { CONTAINS, USES, IMPLEMENTS, DEPENDS_ON, PUBLISHES, SUBSCRIBES_TO,
        ASSOCIATES_WITH, MAPS_TO }
}
