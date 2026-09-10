package org.shark.renovatio.semantic.ir;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class SemanticProgramTest {
    private static final String HASH = "0".repeat(64);
    private static final SourceSpan SPAN = new SourceSpan("src/program.cob", 1, 1, 2, 9);

    @Test
    void identityIsStableAndNormalizesProgramId() {
        String first = SemanticIdentity.nodeId("café", SemanticProgram.NodeKind.TYPE, SPAN, "field:amount");
        String second = SemanticIdentity.nodeId("CAFÉ", SemanticProgram.NodeKind.TYPE, SPAN, "field:amount");
        assertEquals(first, second);
        assertEquals(64, first.length());
    }

    @Test
    void programSortsNodesAndDefensivelyCopies() {
        var typeB = type("B", new SourceSpan("src/program.cob", 2, 1, 2, 2));
        var typeA = type("A", new SourceSpan("src/program.cob", 1, 1, 1, 2));
        var mutable = new java.util.ArrayList<>(List.of(typeB, typeA));
        SemanticProgram program = program(mutable);
        mutable.clear();
        assertEquals(2, program.types().size());
        assertTrue(program.types().get(0).header().id().compareTo(program.types().get(1).header().id()) < 0);
        assertThrows(UnsupportedOperationException.class, () -> program.types().add(typeA));
    }

    @Test
    void rejectsMismatchedIdentityAndInvalidPaths() {
        var wrong = new SemanticProgram.Header("1".repeat(64), SemanticProgram.NodeKind.TYPE, "field:A", SPAN);
        var type = new SemanticProgram.SemanticType(wrong, "A", SemanticProgram.TypeKind.TEXT,
                SemanticProgram.Signedness.UNKNOWN, OptionalInt.empty(), OptionalInt.empty(),
                OptionalInt.empty(), OptionalInt.empty(), List.of());
        assertThrows(IllegalArgumentException.class, () -> program(List.of(type)));
        assertThrows(IllegalArgumentException.class, () -> new SourceSpan("../program.cob", 1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new SourceSpan("program.cob", 0, 1, 1, 1));
    }

    @Test
    void preservesAssumptionOrderButSortsEvidence() {
        var header = SemanticProgram.Header.create("TEST", SemanticProgram.NodeKind.DATA_INTENT,
                "intent:overlay", SPAN);
        var intent = new SemanticProgram.DataIntent(header, "2".repeat(64),
                SemanticProgram.IntentKind.OVERLAPPING_STORAGE, "shared bytes",
                List.of("second", "first"), "3".repeat(64));
        assertEquals(List.of("second", "first"), intent.assumptions());
        var residual = new SemanticProgram.UnclassifiedDataAccess(
                SemanticProgram.Header.create("TEST", SemanticProgram.NodeKind.UNCLASSIFIED_DATA_ACCESS,
                        "access:x", SPAN), "X", "READ", "unknown storage",
                List.of("f".repeat(64), "a".repeat(64)));
        assertEquals(List.of("a".repeat(64), "f".repeat(64)), residual.evidenceIds());
    }

    @Test
    void rejectsDanglingControlFlowAndEffects() {
        var flowNode = new SemanticProgram.ControlFlowNode(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.CONTROL_FLOW_NODE, "paragraph:main", SPAN));
        var badEdge = new SemanticProgram.ControlFlowEdge(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.CONTROL_FLOW_EDGE, "edge:bad", SPAN), flowNode.header().id(),
                "4".repeat(64), SemanticProgram.EdgeKind.SEQUENTIAL);
        assertThrows(IllegalArgumentException.class, () -> program(List.of(),
                new SemanticProgram.ControlFlow(Optional.of(flowNode.header().id()), List.of(flowNode), List.of(badEdge))));

        var effect = new SemanticProgram.SideEffect(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.SIDE_EFFECT, "write:x", SPAN), SemanticProgram.EffectKind.STATE_WRITE,
                List.of("5".repeat(64)), "writes X");
        assertThrows(IllegalArgumentException.class, () -> program(List.of(), List.of(effect)));

        var typeWithMissingMember = new SemanticProgram.SemanticType(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.TYPE, "group:record", SPAN), "RECORD",
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
                List.of("6".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> program(List.of(typeWithMissingMember)));

        var type = type("A", SPAN);
        var intent = new SemanticProgram.DataIntent(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.DATA_INTENT, "intent:missing", SPAN), "7".repeat(64),
                SemanticProgram.IntentKind.OVERLAPPING_STORAGE, "shared bytes", List.of("verified"),
                "8".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> new SemanticProgram("1",
                SemanticProgram.Header.create("TEST", SemanticProgram.NodeKind.PROGRAM, "program", SPAN),
                "test", provenance(), List.of(type), List.of(intent), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of()));
    }

    private static SemanticProgram.SemanticType type(String symbol, SourceSpan span) {
        return new SemanticProgram.SemanticType(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.TYPE, "field:" + symbol, span), symbol,
                SemanticProgram.TypeKind.TEXT, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
    }

    private static SemanticProgram program(List<SemanticProgram.SemanticType> types) {
        return program(types, List.of());
    }

    private static SemanticProgram program(List<SemanticProgram.SemanticType> types,
                                           List<SemanticProgram.SideEffect> effects) {
        return new SemanticProgram("1", SemanticProgram.Header.create("TEST", SemanticProgram.NodeKind.PROGRAM,
                "program", SPAN), "test", provenance(), types, List.of(), effects, List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
    }

    private static SemanticProgram program(List<SemanticProgram.SemanticType> types,
                                           SemanticProgram.ControlFlow flow) {
        return new SemanticProgram("1", SemanticProgram.Header.create("TEST", SemanticProgram.NodeKind.PROGRAM,
                "program", SPAN), "test", provenance(), types, List.of(), List.of(), List.of(), flow, List.of());
    }

    private static SourceProvenance provenance() {
        return new SourceProvenance("src/program.cob", HASH, "COBOL", Optional.of("IBM"), List.of());
    }
}
