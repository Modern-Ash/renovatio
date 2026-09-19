package org.modernash.renovatio.semantic.ir;

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
                              List<UnclassifiedDataAccess> unclassifiedDataAccesses,
                              List<FieldFlow> fieldFlows) {
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
        fieldFlows = nodes(fieldFlows, programId, NodeKind.FIELD_FLOW);

        List<Node> all = new ArrayList<>();
        all.add(headerNode(header));
        all.addAll(types); all.addAll(dataIntents); all.addAll(sideEffects); all.addAll(ioOperations);
        all.addAll(controlFlow.nodes()); all.addAll(controlFlow.edges()); all.addAll(unclassifiedDataAccesses);
        all.addAll(fieldFlows);
        requireUnique(all.stream().map(Node::header).map(Header::id).toList(), "semantic node id");

        Set<String> semanticIds = new HashSet<>(all.stream().map(Node::header).map(Header::id).toList());
        Set<String> typeIds = new HashSet<>(types.stream().map(value -> value.header().id()).toList());
        types.forEach(type -> requireReferences(type.memberIds(), semanticIds, "type member"));
        dataIntents.forEach(intent -> requireReferences(List.of(intent.subjectNodeId()), typeIds,
                "data intent subject"));
        sideEffects.forEach(effect -> requireReferences(effect.affectedNodeIds(), semanticIds, "affected node"));
        Set<String> effectIds = new HashSet<>(sideEffects.stream().map(value -> value.header().id()).toList());
        ioOperations.forEach(io -> requireReferences(io.sideEffectIds(), effectIds, "side effect"));
        fieldFlows.forEach(flow -> requireReferences(List.of(flow.sourceTypeId(), flow.targetTypeId()), typeIds,
                "field flow endpoint"));
    }

    public SemanticProgram(String schemaVersion, Header header, String programId,
                           SourceProvenance sourceProvenance, List<SemanticType> types,
                           List<DataIntent> dataIntents, List<SideEffect> sideEffects,
                           List<IoOperation> ioOperations, ControlFlow controlFlow,
                           List<UnclassifiedDataAccess> unclassifiedDataAccesses) {
        this(schemaVersion, header, programId, sourceProvenance, types, dataIntents, sideEffects, ioOperations,
                controlFlow, unclassifiedDataAccesses, List.of());
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
        CONTROL_FLOW_NODE, CONTROL_FLOW_EDGE, UNCLASSIFIED_DATA_ACCESS, FIELD_FLOW }
    public enum TypeKind { TEXT, INTEGER, DECIMAL, BOOLEAN, GROUP, UNKNOWN }
    public enum Signedness { SIGNED, UNSIGNED, UNKNOWN }
    public enum IntentKind { OVERLAPPING_STORAGE, DEPENDENT_CARDINALITY, MOVE_CORRESPONDING }
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
                              List<String> sideEffectIds, Optional<String> boundRecordSymbol,
                              List<String> keyFieldSymbols, Optional<String> assignTarget) implements Node {
        public IoOperation {
            Objects.requireNonNull(ioKind, "ioKind"); operation = SemanticIdentity.text(operation, "operation");
            resourceReference = resourceReference == null ? Optional.empty()
                    : resourceReference.map(value -> SemanticIdentity.text(value, "resourceReference"));
            Objects.requireNonNull(direction, "direction");
            sideEffectIds = sorted(sideEffectIds, "sideEffectId");
            // The record symbol a FILE-kind resource is structurally bound
            // to — e.g. the FD's "RECORD IS ..." / the 01-level record
            // declared right under it in the FILE SECTION — when the
            // provider was able to parse that binding directly. Lets a
            // domain projector associate a file with its record exactly,
            // rather than guessing from name similarity (see #280 gap:
            // name-based matching missed pairs like ACCTFILE-FILE /
            // ACCOUNT-RECORD or DALYTRAN-FILE / TRAN-RECORD, where COBOL
            // naming conventions diverge between the FD and the record).
            boundRecordSymbol = boundRecordSymbol == null ? Optional.empty()
                    : boundRecordSymbol.map(value -> SemanticIdentity.text(value, "boundRecordSymbol"));
            // The field(s) declared as this FILE-kind resource's RECORD
            // KEY / ALTERNATE RECORD KEY in FILE-CONTROL — a VSAM/indexed
            // file's actual access key(s). Real structural evidence for
            // inferring a relation between two *different* entities (one
            // file's own field happens to be another file's declared key
            // -> the first references the second), unlike anything a name
            // heuristic alone could establish.
            keyFieldSymbols = sorted(keyFieldSymbols, "keyFieldSymbol");
            // The physical dataset this FILE-kind resource is ASSIGN TO —
            // the same physical VSAM/indexed file is often declared under a
            // different local SELECT name in every program that opens it
            // (ACCT-FILE, ACCOUNT-FILE, ACCTFILE-FILE all ASSIGN TO
            // ACCTFILE). Real structural evidence those are one physical
            // resource, used to merge them before FK inference runs so a
            // same-file duplicate doesn't get mistaken for a foreign key.
            assignTarget = assignTarget == null ? Optional.empty()
                    : assignTarget.map(value -> SemanticIdentity.text(value, "assignTarget"));
        }

        public IoOperation(Header header, IoKind ioKind, String operation,
                           Optional<String> resourceReference, Direction direction,
                           List<String> sideEffectIds) {
            this(header, ioKind, operation, resourceReference, direction, sideEffectIds, Optional.empty(), List.of(),
                    Optional.empty());
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

    // A single MOVE (or equivalent assignment)'s source -> target field
    // pair, both resolved to real SemanticType ids. This is the actual
    // procedure-division data flow a COBOL program performs — e.g. "MOVE
    // XREF-ACCT-ID TO WS-ACCT-ID" right before a keyed READ — and is what
    // lets a domain projector infer a relation between two different
    // business entities from the program's own logic, rather than from
    // name similarity or a coincidental identical field name.
    public record FieldFlow(Header header, String sourceTypeId, String targetTypeId) implements Node {
        public FieldFlow {
            sourceTypeId = SemanticIdentity.hash(sourceTypeId, "sourceTypeId");
            targetTypeId = SemanticIdentity.hash(targetTypeId, "targetTypeId");
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
