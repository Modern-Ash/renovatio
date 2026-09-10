package org.shark.renovatio.cobol.ir.annotated;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.*;
import org.shark.renovatio.cobol.ir.context.CobolExecutionContext;
import org.shark.renovatio.cobol.ir.flow.ControlFlowGraph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobolIrIdentityProjectorTest {

    private final CobolIrIdentityProjector projector = new CobolIrIdentityProjector();

    @Test
    void baseHashIsStableAcrossObjectMemberOrder() {
        CobolParagraph alpha = CobolParagraph.empty("ALPHA");
        CobolParagraph omega = CobolParagraph.empty("OMEGA");
        CobolIntermediateModel first = CobolIntermediateModel.builder().programId("billing")
                .addParagraph(alpha).addParagraph(omega).build();
        CobolIntermediateModel reordered = CobolIntermediateModel.builder().programId("billing")
                .addParagraph(omega).addParagraph(alpha).build();

        assertEquals(projector.baseIrHash(first), projector.baseIrHash(reordered));
    }

    @Test
    void identicalTextAtDifferentPathsHasDifferentNodeIds() {
        Map<String, Object> content = Map.of("name", "1000-PROC", "statements", List.of());

        String first = projector.nodeId(AnnotatedNodeKind.PARAGRAPH, "/paragraphs/1000-PROC", null, content);
        String second = projector.nodeId(AnnotatedNodeKind.PARAGRAPH, "/paragraphs/COPY~1PROC", null, content);

        assertNotEquals(first, second);
        assertEquals(first, projector.nodeId(AnnotatedNodeKind.PARAGRAPH, "/paragraphs/1000-PROC", null, content));
    }

    @Test
    void contentAndSourceSpanInvalidateNodeIdentity() {
        Map<String, Object> original = Map.of("source", "A", "target", "B");
        SourceSpan firstSpan = new SourceSpan("sample.cbl", 10, 1, 10, 12);

        String first = projector.nodeId(AnnotatedNodeKind.MOVE_STATEMENT, "/paragraphs/MAIN/statements/0", firstSpan, original);

        assertNotEquals(first, projector.nodeId(AnnotatedNodeKind.MOVE_STATEMENT,
                "/paragraphs/MAIN/statements/0", firstSpan, Map.of("source", "C", "target", "B")));
        assertNotEquals(first, projector.nodeId(AnnotatedNodeKind.MOVE_STATEMENT,
                "/paragraphs/MAIN/statements/0", new SourceSpan("sample.cbl", 11, 1, 11, 12), original));
    }

    @Test
    void pointerSegmentsUseRfc6901Escaping() {
        assertEquals("/paragraphs/A~1B~0C", CobolIrIdentityProjector.childPointer("/paragraphs", "A/B~C"));
        assertThrows(IllegalArgumentException.class,
                () -> projector.nodeId(AnnotatedNodeKind.PARAGRAPH, "paragraphs/MAIN", null, Map.of()));
    }

    @Test
    void projectsAndEnumeratesEveryNestedBaseIrNodeDeterministically() {
        Level88Condition active = new Level88Condition("ACTIVE", "STATUS", List.of(Level88Value.exact("A")));
        CobolDataItem status = new CobolDataItem("STATUS", "X", 1, null, null, "String", null, List.of(active));
        CobolParagraph paragraph = new CobolParagraph("A/B~C", List.of(
                new IfStatement("READY", List.of(new MoveStatement("A", "B")), List.of()),
                new EvaluateStatement("STATUS", List.of(
                        new EvaluateStatement.EvaluateWhenBranch("A", List.of(new PerformStatement("MAIN", null)))))));
        CobolIntermediateModel model = CobolIntermediateModel.builder().programId("sample")
                .dataItems(List.of(status)).addParagraph(paragraph).build();

        List<CobolIrIdentityProjector.ProjectedNode> first = projector.nodes(model);

        assertEquals(9, first.size());
        assertEquals(List.of(
                        AnnotatedNodeKind.DATA_ITEM, AnnotatedNodeKind.LEVEL_88_CONDITION,
                        AnnotatedNodeKind.LEVEL_88_VALUE, AnnotatedNodeKind.PARAGRAPH,
                        AnnotatedNodeKind.IF_STATEMENT, AnnotatedNodeKind.MOVE_STATEMENT,
                        AnnotatedNodeKind.EVALUATE_STATEMENT, AnnotatedNodeKind.EVALUATE_BRANCH,
                        AnnotatedNodeKind.PERFORM_STATEMENT),
                first.stream().map(CobolIrIdentityProjector.ProjectedNode::nodeKind).toList());
        assertEquals("/paragraphs/A~1B~0C", first.get(3).pointer());
        assertEquals(first, projector.nodes(model));
        assertEquals(projector.baseIrHash(model), AnnotatedIdentity.hashCanonical(projector.project(model)));
    }

    @Test
    void mapsTypedExpressionAndConditionNodesWithoutDuplicatingSourceSpan() {
        SourceSpan span = new SourceSpan("sample.cbl", 4, 2, 4, 16);
        ComparisonCondition condition = new ComparisonCondition(
                new DataReferenceExpression("amount", span),
                ComparisonCondition.ComparisonOperator.GREATER_THAN,
                new LiteralExpression(LiteralExpression.LiteralKind.NUMERIC, "0", span), span);

        CobolIrIdentityProjector.ProjectedNode projected = projector.node(condition, "/conditions/0");

        assertEquals(AnnotatedNodeKind.COMPARISON_CONDITION, projected.nodeKind());
        assertEquals(Map.of("left", Map.of("dataName", "AMOUNT"), "operator", "GREATER_THAN",
                "right", Map.of("kind", "NUMERIC", "value", "0")), projected.semanticContent());
        assertThrows(IllegalArgumentException.class, () -> projector.node(new Object(), "/unknown/0"));
    }

    @Test
    void completeBaseHashChangesWithContextFlowAndDerivedViews() {
        CobolIntermediateModel baseline = CobolIntermediateModel.builder().programId("sample").build();
        ControlBreakPattern pattern = ControlBreakPattern.builder().patternId("P1").fileName("INPUT")
                .initializationStatements(List.of(new MoveStatement("ZERO", "TOTAL"))).build();
        DecomposedBusinessLogic logic = DecomposedBusinessLogic.builder().programId("SAMPLE")
                .metadata(Map.of("strategy", "stream", "version", 1)).build();
        CobolIntermediateModel enriched = CobolIntermediateModel.builder().programId("sample")
                .controlFlowGraph(ControlFlowGraph.builder().addEdge("MAIN", "END").build())
                .executionContext(CobolExecutionContext.builder().registerVariable("TOTAL", "working-storage")
                        .attribute("flags", List.of("batch", "audit")).build())
                .controlBreakPatterns(List.of(pattern)).decomposedLogic(logic).build();

        assertNotEquals(projector.baseIrHash(baseline), projector.baseIrHash(enriched));
        assertEquals(List.of("END"), ((Map<?, ?>) ((Map<?, ?>) projector.project(enriched)
                .get("controlFlowGraph")).get("adjacency")).get("MAIN"));
    }
}
