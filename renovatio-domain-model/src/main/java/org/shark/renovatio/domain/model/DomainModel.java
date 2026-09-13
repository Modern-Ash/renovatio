package org.shark.renovatio.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HexFormat;

/** Immutable v1 business model independent of target architecture and language. */
public record DomainModel(String schemaVersion, String projectId, List<DomainNode> nodes,
                          List<DomainRelation> relations, List<BusinessInvariant> invariants,
                          Map<String, LayoutPosition> layout, List<ExcludedNode> excludedNodeIds) {
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
        layout = normalizeLayout(layout, ids);
        excludedNodeIds = normalizeExcluded(excludedNodeIds, ids);
    }

    public DomainModel(String schemaVersion, String projectId, List<DomainNode> nodes,
                       List<DomainRelation> relations, List<BusinessInvariant> invariants) {
        this(schemaVersion, projectId, nodes, relations, invariants, Map.of(), List.of());
    }

    private static Set<String> uniqueIds(List<String> values, String label) {
        Set<String> ids = new HashSet<>(values);
        if (ids.size() != values.size()) throw new IllegalArgumentException("duplicate " + label + " id");
        return Set.copyOf(ids);
    }

    public String canonicalHash() {
        String canonical = schemaVersion + "\n" + projectId + "\n"
                + nodes + "\n" + relations + "\n" + invariants + "\n" + layout + "\n" + excludedNodeIds;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Map<String, LayoutPosition> normalizeLayout(Map<String, LayoutPosition> values, Set<String> nodeIds) {
        if (values == null || values.isEmpty()) return Map.of();
        Map<String, LayoutPosition> result = new LinkedHashMap<>();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String id = text(entry.getKey(), "layout id");
            if (!nodeIds.contains(id)) throw new IllegalArgumentException("layout references unknown node");
            result.put(id, Objects.requireNonNull(entry.getValue(), "layout position"));
        });
        return Collections.unmodifiableMap(result);
    }

    private static List<ExcludedNode> normalizeExcluded(List<ExcludedNode> values, Set<String> nodeIds) {
        if (values == null || values.isEmpty()) return List.of();
        Set<String> ids = uniqueIds(values.stream().map(ExcludedNode::id).toList(), "excluded node");
        if (!nodeIds.containsAll(ids)) throw new IllegalArgumentException("excludedNodeIds references unknown node");
        return values.stream().sorted(Comparator.comparing(ExcludedNode::id)).toList();
    }

    private static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record DomainNode(String id, Kind kind, String name, List<Property> properties,
                             List<Evidence> evidence, Origin origin, double confidence,
                             String tableName, String sourceDataset) {
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
            tableName = blankToNull(tableName);
            sourceDataset = blankToNull(sourceDataset);
        }

        public DomainNode(String id, Kind kind, String name, List<Property> properties,
                          List<Evidence> evidence, Origin origin, double confidence) {
            this(id, kind, name, properties, evidence, origin, confidence, null, null);
        }

        public DomainNode(String id, Kind kind, String name, List<Evidence> evidence,
                          Origin origin, double confidence) {
            this(id, kind, name, List.of(), evidence, origin, confidence, null, null);
        }
    }

    public record Property(String name, String type, boolean required, List<Evidence> evidence,
                           boolean isKey, String columnName, String sourceColumn, String sourceDataset) {
        public Property {
            name = text(name, "property name");
            type = text(type, "property type");
            evidence = evidence == null ? List.of() : evidence.stream()
                    .sorted(Comparator.comparing(Evidence::sourceRef)).toList();
            columnName = blankToNull(columnName);
            sourceColumn = blankToNull(sourceColumn);
            sourceDataset = blankToNull(sourceDataset);
        }

        public Property(String name, String type, boolean required, List<Evidence> evidence) {
            this(name, type, required, evidence, false, null, null, null);
        }
    }

    public record DomainRelation(String id, String fromId, String toId, RelationKind kind,
                                 Cardinality sourceCardinality, Cardinality targetCardinality,
                                 ForeignKey foreignKey) {
        public DomainRelation {
            id = text(id, "id"); fromId = text(fromId, "fromId"); toId = text(toId, "toId");
            Objects.requireNonNull(kind, "kind");
            sourceCardinality = sourceCardinality == null ? Cardinality.ONE : sourceCardinality;
            targetCardinality = targetCardinality == null ? Cardinality.ONE : targetCardinality;
        }

        public DomainRelation(String id, String fromId, String toId, RelationKind kind,
                              Cardinality sourceCardinality, Cardinality targetCardinality) {
            this(id, fromId, toId, kind, sourceCardinality, targetCardinality, null);
        }

        public DomainRelation(String id, String fromId, String toId, RelationKind kind) {
            this(id, fromId, toId, kind, Cardinality.ONE, Cardinality.ONE, null);
        }
    }

    public record ForeignKey(String property, String referencesProperty) {
        public ForeignKey {
            property = text(property, "foreign key property");
            referencesProperty = text(referencesProperty, "foreign key referencesProperty");
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

    public record LayoutPosition(double x, double y) {
        public LayoutPosition {
            if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("layout coordinates must be finite");
        }
    }

    public record ExcludedNode(String id, String reason) {
        public ExcludedNode {
            id = text(id, "excluded node id");
            reason = reason == null || reason.isBlank() ? "Excluded from generation" : reason.trim();
        }
    }

    public enum Origin { DETERMINISTIC, LLM, HUMAN }
    public enum Kind { ENTITY, VALUE_OBJECT, AGGREGATE, USE_CASE, DOMAIN_SERVICE, REPOSITORY,
        EXTERNAL_SYSTEM, EVENT, BOUNDED_CONTEXT }
    public enum Cardinality { ONE, ZERO_OR_ONE, ONE_OR_MORE, ZERO_OR_MORE }
    public enum RelationKind { CONTAINS, USES, IMPLEMENTS, DEPENDS_ON, PUBLISHES, SUBSCRIBES_TO,
        ASSOCIATES_WITH, MAPS_TO }
}
