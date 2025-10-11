package org.shark.renovatio.cobol.ir.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelClassesTest {

    @Test
    void moveStatement_shouldHoldValues_andRejectNulls() {
        MoveStatement m = new MoveStatement("A", "B");
        assertEquals("A", m.getSource());
        assertEquals("B", m.getTarget());
        assertThrows(NullPointerException.class, () -> new MoveStatement(null, "B"));
        assertThrows(NullPointerException.class, () -> new MoveStatement("A", null));
    }

    @Test
    void computeStatement_shouldHoldValues_andRejectNulls() {
        ComputeStatement c = new ComputeStatement("T", "X+1");
        assertEquals("T", c.getTarget());
        assertEquals("X+1", c.getExpression());
        assertThrows(NullPointerException.class, () -> new ComputeStatement(null, "X"));
        assertThrows(NullPointerException.class, () -> new ComputeStatement("T", null));
    }

    @Test
    void fileOperationStatement_shouldHoldValues_andRejectNulls() {
        for (FileOperationStatement.OperationType t : FileOperationStatement.OperationType.values()) {
            FileOperationStatement f = new FileOperationStatement(t, "FILEA");
            assertEquals(t, f.getOperationType());
            assertEquals("FILEA", f.getFileName());
        }
        assertThrows(NullPointerException.class, () -> new FileOperationStatement(null, "X"));
        assertThrows(NullPointerException.class, () -> new FileOperationStatement(FileOperationStatement.OperationType.OPEN, null));
    }

    @Test
    void performStatement_shouldUppercaseParagraph_andAllowNullThrough() {
        PerformStatement p1 = new PerformStatement("para-1", null);
        assertEquals("PARA-1", p1.getParagraph());
        assertNull(p1.getThroughParagraph());
        PerformStatement p2 = new PerformStatement("para-1", "para-2");
        assertEquals("PARA-2", p2.getThroughParagraph());
    }

    @Test
    void ifStatement_shouldCopyLists_andRejectNullCondition() {
        IfStatement s = new IfStatement("A > B", List.of(new MoveStatement("1","X")), null);
        assertEquals("A > B", s.getCondition());
        assertEquals(1, s.getThenStatements().size());
        assertNotNull(s.getElseStatements());
        assertThrows(NullPointerException.class, () -> new IfStatement(null, List.of(), List.of()));
    }

    @Test
    void callStatement_shouldCopyArgs_andRejectNullTarget() {
        CallStatement c1 = new CallStatement("SUBP", List.of("A","B"));
        assertEquals("SUBP", c1.target());
        assertEquals(List.of("A","B"), c1.arguments());
        CallStatement c2 = new CallStatement("SUBP", null);
        assertEquals(List.of(), c2.arguments());
        assertThrows(NullPointerException.class, () -> new CallStatement(null, List.of()));
    }

    @Test
    void evaluateStatement_andBranches_shouldCopy_andRejectNulls() {
        EvaluateStatement.EvaluateWhenBranch br = new EvaluateStatement.EvaluateWhenBranch("X", null);
        assertEquals("X", br.getCondition());
        assertEquals(List.of(), br.getStatements());
        EvaluateStatement ev = new EvaluateStatement("A", List.of(br));
        assertEquals("A", ev.getExpression());
        assertEquals(1, ev.getBranches().size());
        assertThrows(NullPointerException.class, () -> new EvaluateStatement(null, List.of()));
        assertThrows(NullPointerException.class, () -> new EvaluateStatement.EvaluateWhenBranch(null, List.of()));
    }

    @Test
    void cobolParagraph_shouldUppercaseName_andCopyStatements_andSupportEmpty() {
        CobolParagraph empty = CobolParagraph.empty("main");
        assertEquals("MAIN", empty.getName());
        assertNotNull(empty.getStatements());
        CobolParagraph p = new CobolParagraph("para-1", null);
        assertEquals("PARA-1", p.getName());
        assertEquals(List.of(), p.getStatements());
    }

    @Test
    void db2Statement_shouldHoldSql_andRejectNull() {
        Db2Statement s = new Db2Statement("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        assertEquals("SELECT 1 FROM SYSIBM.SYSDUMMY1", s.getSql());
        assertThrows(NullPointerException.class, () -> new Db2Statement(null));
    }

    @Test
    void cobolDataItem_shouldExposeOptionalFields_andDefaultJavaType() {
        CobolDataItem item = new CobolDataItem("NAME", "X(10)", 1, null, null, null);
        assertEquals("NAME", item.getName());
        assertEquals("X(10)", item.getPicture());
        assertEquals(1, item.getLevel());
        assertTrue(item.getOccurs().isEmpty());
        assertTrue(item.getRedefines().isEmpty());
        assertEquals("String", item.getJavaType());
        CobolDataItem item2 = new CobolDataItem("NUM", "9(2)", 1, 5, "OLD", "Integer");
        assertEquals(5, item2.getOccurs().orElseThrow());
        assertEquals("OLD", item2.getRedefines().orElseThrow());
        assertEquals("Integer", item2.getJavaType());
    }

    @Test
    void cobolIntermediateModel_builder_shouldBuild_andResolve() {
        CobolParagraph p1 = CobolParagraph.empty("main");
        CobolIntermediateModel m = CobolIntermediateModel.builder()
                .programId("sample")
                .addParagraph(p1)
                .dataItems(List.of(new CobolDataItem("A","X(1)",1,null,null,null)))
                .controlFlowGraph(org.shark.renovatio.cobol.ir.flow.ControlFlowGraph.empty())
                .executionContext(org.shark.renovatio.cobol.ir.context.CobolExecutionContext.empty())
                .build();
        assertEquals("SAMPLE", m.getProgramId());
        assertEquals(p1, m.findParagraph("main").orElseThrow());
        assertTrue(m.findParagraph(null).isEmpty());
        assertEquals("MAIN", m.getEntryParagraph().getName());
        // When map is empty, getEntryParagraph returns MAIN
        CobolIntermediateModel m2 = CobolIntermediateModel.builder().programId("x").build();
        assertEquals("MAIN", m2.getEntryParagraph().getName());
        assertEquals(0, m2.getParagraphs().size());
    }

    @Test
    void controlFlowGraph_builder_shouldAddEdges_andProvideSuccessors() {
        org.shark.renovatio.cobol.ir.flow.ControlFlowGraph g = org.shark.renovatio.cobol.ir.flow.ControlFlowGraph.builder()
                .ensureNode("A").addEdge("A", "B").addEdge("B", "C").addEdge(null, "X").addEdge("Y", null)
                .build();
        assertEquals(Map.of(
                "A", java.util.Set.of("B"),
                "B", java.util.Set.of("C"),
                "C", java.util.Set.of()
        ), g.getAdjacency());
        assertEquals(java.util.Set.of("B"), g.successors("a"));
        assertEquals(java.util.Set.of(), g.successors(null));
    }
}

