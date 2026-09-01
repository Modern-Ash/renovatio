package org.shark.renovatio.semantic.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;

/** Immutable version-1 target-neutral semantic program. */
public record SemanticProgram(String schemaVersion, Header header, String programId,
                              SourceProvenance sourceProvenance, List<SemanticType> types,
                              List<DataIntent> dataIntents, List<SideEffect> sideEffects,
                              List<IoOperation> ioOperations, ControlFlow controlFlow,
                              List<UnclassifiedDataAccess> unclassifiedDataAccesses) {
    public static final String SCHEMA_VERSION = "1";

    public SemanticProgram {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("unsupported schemaVersion");
        programId = SemanticIdentity.normalizeProgramId(programId);
        header = checkedHeader(header, programId, NodeKind.PROGRAM);
        sourceProvenance = Objects.requireNonNull(sourceProvenance, "sourceProvenance");
        if (!header.sourceSpan().sourcePath().equals(sourceProvenance.sourcePath())) {
            throw new IllegalArgumentException("program span and provenance paths must match");
        }
        types = nodes(types, programId, NodeKind.TYPE);
        dataIntents = nodes(dataIntents, programId, NodeKind.DATA_INTENT);
        sideEffects = nodes(sideEffects, programId, NodeKind.SIDE_EFFECT);
        ioOperations = nodes(ioOperations, programId, NodeKind.IO_OPERATION);
        controlFlow = Objects.requireNonNull(controlFlow, "controlFlow").validated(programId);
        unclassifiedDataAccesses = nodes(unclassifiedDataAccesses, programId, NodeKind.UNCLASSIFIED_DATA_ACCESS);

        List<Node> all = new ArrayList<>();
        all.add(headerNode(header));
        all.addAll(types); all.addAll(dataIntents); all.addAll(sideEffects); all.addAll(ioOperations);
        all.addAll(controlFlow.nodes()); all.addAll(controlFlow.edges()); all.addAll(unclassifiedDataAccesses);
        requireUnique(all.stream().map(Node::header).map(Header::id).toList(), "semantic node id");

        Set<String> semanticIds = new HashSet<>(all.stream().map(Node::header).map(Header::id).toList());
        types.forEach(type -> requireReferences(type.memberIds(), semanticIds, "type member"));
        sideEffects.forEach(effect -> requireReferences(effect.affectedNodeIds(), semanticIds, "affected node"));
        Set<String> effectIds = new HashSet<>(sideEffects.stream().map(value -> value.header().id()).toList());
        ioOperations.forEach(io -> requireReferences(io.sideEffectIds(), effectIds, "side effect"));
    }

    private static Node headerNode(Header value) { return () -> value; }

    private static <T extends Node> List<T> nodes(Collection<T> values, String programId, NodeKind kind) {
        List<T> copy = values == null ? List.of() : List.copyOf(values);
        copy.forEach(value -> checkedHeader(value.header(), programId, kind));
        return copy.stream().sorted(Comparator.comparing(value -> value.header().id())).toList();
    }

    private static Header checkedHeader(Header header, String programId, NodeKind kind) {
        Objects.requireNonNull(header, "header");
        if (header.kind() != kind) throw new IllegalArgumentException("expected node kind " + kind);
        String expected = SemanticIdentity.nodeId(programId, kind, header.sourceSpan(), header.semanticRole());
        if (!expected.equals(header.id())) throw new IllegalArgumentException("node id does not match semantic identity");
        return header;
    }

    private static void requireUnique(List<String> values, String label) {
        if (new HashSet<>(values).size() != values.size()) throw new IllegalArgumentException("duplicate " + label);
    }

    private static void requireReferences(List<String> values, Set<String> available, String label) {
        values.forEach(value -> { if (!available.contains(value)) throw new IllegalArgumentException("dangling " + label); });
    }

    public interface Node { Header header(); }

    public record Header(String id, NodeKind kind, String semanticRole, SourceSpan sourceSpan) {
        public Header {
            id = SemanticIdentity.hash(id, "id");
            Objects.requireNonNull(kind, "kind");
            semanticRole = SemanticIdentity.text(semanticRole, "semanticRole");
            Objects.requireNonNull(sourceSpan, "sourceSpan");
        }
        public static Header create(String programId, NodeKind kind, String role, SourceSpan span) {
            return new Header(SemanticIdentity.nodeId(programId, kind, span, role), kind, role, span);
        }
    }

    public enum NodeKind { PROGRAM, TYPE, DATA_INTENT, SIDE_EFFECT, IO_OPERATION,
        CONTROL_FLOW_NODE, CONTROL_FLOW_EDGE, UNCLASSIFIED_DATA_ACCESS }
    public enum TypeKind { TEXT, INTEGER, DECIMAL, BOOLEAN, GROUP, UNKNOWN }
    public enum Signedness { SIGNED, UNSIGNED, UNKNOWN }
    public enum IntentKind { OVERLAPPING_STORAGE, DEPENDENT_CARDINALITY }
    public enum EffectKind { STATE_READ, STATE_WRITE, EXTERNAL_CALL, UNKNOWN }
    public enum IoKind { FILE, DATABASE, TERMINAL, TRANSACTION, MESSAGE, UNKNOWN }
    public enum Direction { READ, WRITE, READ_WRITE, UNKNOWN }
    public enum EdgeKind { SEQUENTIAL, BRANCH_TRUE, BRANCH_FALSE, CALL, RETURN, LOOP, UNKNOWN }

    public record SemanticType(Header header, String symbol, TypeKind typeKind, Signedness signedness,
                               OptionalInt precision, OptionalInt scale, OptionalInt minCardinality,
                               OptionalInt maxCardinality, List<String> memberIds) implements Node {
        public SemanticType {
            symbol = SemanticIdentity.text(symbol, "symbol");
            Objects.requireNonNull(typeKind, "typeKind"); Objects.requireNonNull(signedness, "signedness");
            precision = optional(precision); scale = optional(scale);
            minCardinality = optional(minCardinality); maxCardinality = optional(maxCardinality);
            if (precision.isPresent() && precision.getAsInt() < 1) throw new IllegalArgumentException("precision must be positive");
            if (scale.isPresent() && scale.getAsInt() < 0) throw new IllegalArgumentException("scale must not be negative");
            if (precision.isPresent() && scale.isPresent() && scale.getAsInt() > precision.getAsInt())
                throw new IllegalArgumentException("scale exceeds precision");
            if (minCardinality.isPresent() && minCardinality.getAsInt() < 0) throw new IllegalArgumentException("minimum cardinality must not be negative");
            if (maxCardinality.isPresent() && (!minCardinality.isPresent() || maxCardinality.getAsInt() < minCardinality.getAsInt()))
                throw new IllegalArgumentException("invalid cardinality bounds");
            memberIds = sorted(memberIds, "memberId");
        }
    }

    public record DataIntent(Header header, String subjectNodeId, IntentKind intentKind,
                             String interpretation, List<String> assumptions,
                             String evidenceId) implements Node {
        public DataIntent {
            subjectNodeId = SemanticIdentity.hash(subjectNodeId, "subjectNodeId");
            Objects.requireNonNull(intentKind, "intentKind");
            interpretation = SemanticIdentity.text(interpretation, "interpretation");
            assumptions = textList(assumptions, "assumption", false);
            if (assumptions.isEmpty()) throw new IllegalArgumentException("assumptions must not be empty");
            evidenceId = SemanticIdentity.hash(evidenceId, "evidenceId");
        }
    }

    public record SideEffect(Header header, EffectKind effectKind, List<String> affectedNodeIds,
                             String description) implements Node {
        public SideEffect {
            Objects.requireNonNull(effectKind, "effectKind");
            affectedNodeIds = sorted(affectedNodeIds, "affectedNodeId");
            description = SemanticIdentity.text(description, "description");
        }
    }

    public record IoOperation(Header header, IoKind ioKind, String operation,
                              Optional<String> resourceReference, Direction direction,
                              List<String> sideEffectIds) implements Node {
        public IoOperation {
            Objects.requireNonNull(ioKind, "ioKind"); operation = SemanticIdentity.text(operation, "operation");
            resourceReference = resourceReference == null ? Optional.empty()
                    : resourceReference.map(value -> SemanticIdentity.text(value, "resourceReference"));
            Objects.requireNonNull(direction, "direction");
            sideEffectIds = sorted(sideEffectIds, "sideEffectId");
        }
    }

    public record ControlFlowNode(Header header) implements Node { }
    public record ControlFlowEdge(Header header, String fromId, String toId, EdgeKind edgeKind) implements Node {
        public ControlFlowEdge {
            fromId = SemanticIdentity.hash(fromId, "fromId"); toId = SemanticIdentity.hash(toId, "toId");
            Objects.requireNonNull(edgeKind, "edgeKind");
        }
    }

    public record ControlFlow(Optional<String> entryNodeId, List<ControlFlowNode> nodes,
                              List<ControlFlowEdge> edges) {
        public ControlFlow {
            entryNodeId = entryNodeId == null ? Optional.empty()
                    : entryNodeId.map(value -> SemanticIdentity.hash(value, "entryNodeId"));
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
            edges = edges == null ? List.of() : List.copyOf(edges);
        }
        private ControlFlow validated(String programId) {
            List<ControlFlowNode> orderedNodes = SemanticProgram.nodes(nodes, programId, NodeKind.CONTROL_FLOW_NODE);
            List<ControlFlowEdge> orderedEdges = SemanticProgram.nodes(edges, programId, NodeKind.CONTROL_FLOW_EDGE)
                    .stream().sorted(Comparator.comparing(ControlFlowEdge::fromId).thenComparing(ControlFlowEdge::toId)
                            .thenComparing(value -> value.edgeKind().name()).thenComparing(value -> value.header().id())).toList();
            Set<String> ids = new HashSet<>(orderedNodes.stream().map(value -> value.header().id()).toList());
            entryNodeId.ifPresent(value -> { if (!ids.contains(value)) throw new IllegalArgumentException("dangling entry node"); });
            orderedEdges.forEach(edge -> { if (!ids.contains(edge.fromId()) || !ids.contains(edge.toId()))
                throw new IllegalArgumentException("dangling control-flow edge"); });
            requireUnique(orderedEdges.stream().map(edge -> edge.fromId() + "\n" + edge.toId() + "\n" + edge.edgeKind()).toList(), "control-flow edge");
            return new ControlFlow(entryNodeId, orderedNodes, orderedEdges);
        }
    }

    public record UnclassifiedDataAccess(Header header, String subject, String observedOperation,
                                         String reason, List<String> evidenceIds) implements Node {
        public UnclassifiedDataAccess {
            subject = SemanticIdentity.text(subject, "subject");
            observedOperation = SemanticIdentity.text(observedOperation, "observedOperation");
            reason = SemanticIdentity.text(reason, "reason");
            evidenceIds = sorted(evidenceIds, "evidenceId");
        }
    }

    private static OptionalInt optional(OptionalInt value) { return value == null ? OptionalInt.empty() : value; }
    private static List<String> sorted(List<String> values, String name) {
        return textList(values, name, true).stream().distinct().sorted().toList();
    }
    private static List<String> textList(List<String> values, String name, boolean allowEmpty) {
        List<String> copy = (values == null ? List.<String>of() : values).stream()
                .map(value -> SemanticIdentity.text(value, name)).toList();
        if (!allowEmpty && copy.isEmpty()) throw new IllegalArgumentException(name + " list must not be empty");
        return copy;
    }
}
