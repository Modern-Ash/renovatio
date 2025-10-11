package org.shark.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.flow.ControlFlowGraph;
import org.shark.renovatio.cobol.ir.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SimpleCobolIrParserAdvancedTest {

    @Test
    void parse_shouldHandle_ifElse_evaluate_perform_call_execSql_fileOps_and_arithmetic() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. DEMO.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-NUM PIC 9(9).
                01 WS-TXT PIC X(10).
                PROCEDURE DIVISION.
                PARA-1.
                    MOVE 'X' TO WS-TXT.
                    IF WS-NUM > 0 THEN
                        ADD 1 TO WS-NUM
                    ELSE
                        SUBTRACT 1 FROM WS-NUM
                    END-IF.
                    EVALUATE WS-NUM
                        WHEN 0
                            MOVE 'ZERO' TO WS-TXT
                        WHEN OTHER
                            MOVE 'NZ' TO WS-TXT
                    END-EVALUATE.
                    PERFORM PARA-2 THRU PARA-3.
                    CALL "SUBPGM" USING WS-NUM WS-TXT.
                    EXEC SQL
                        SELECT COL FROM TAB
                    END-EXEC.
                    READ INPUTFILE.
                    WRITE OUTFILE.
                    COMPUTE WS-NUM = WS-NUM + 1.
                    ADD 2 3 GIVING SUM.
                    SUBTRACT 1 FROM VALUE GIVING DIFF.
                    MULTIPLY 2 BY FACT.
                    MULTIPLY 2 BY 3 GIVING PROD.
                    DIVIDE 2 INTO QUOT.
                    DIVIDE 10 BY 2 GIVING QUOT2.
                PARA-2.
                    MOVE 'A' TO WS-TXT.
                PARA-3.
                    MOVE 'B' TO WS-TXT.
                """;

        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(cobol);

        assertEquals("DEMO", model.getProgramId());
        assertEquals(2, model.getDataItems().size());
        Map<String, CobolParagraph> paras = model.getParagraphs();
        assertTrue(paras.containsKey("PARA-1"));
        assertTrue(paras.containsKey("PARA-2"));
        assertTrue(paras.containsKey("PARA-3"));

        CobolParagraph p1 = paras.get("PARA-1");
        // Validate that different statement types are present
        assertTrue(p1.getStatements().stream().anyMatch(s -> s instanceof MoveStatement));
        assertTrue(p1.getStatements().stream().anyMatch(s -> s instanceof IfStatement));
        assertTrue(p1.getStatements().stream().anyMatch(s -> s instanceof EvaluateStatement));
        assertTrue(p1.getStatements().stream().anyMatch(s -> s instanceof PerformStatement));
        assertTrue(p1.getStatements().stream().anyMatch(s -> s instanceof CallStatement));
        assertTrue(p1.getStatements().stream().anyMatch(s -> s instanceof Db2Statement));
        // File operations are validated below in cobol2 scenario
        assertTrue(p1.getStatements().stream().anyMatch(s -> s instanceof ComputeStatement));

        // Check cleaned SQL content
        Db2Statement sql = (Db2Statement) p1.getStatements().stream()
                .filter(s -> s instanceof Db2Statement)
                .findFirst().orElseThrow();
        assertEquals("SELECT COL FROM TAB", sql.getSql());

        // Check Evaluate branches collected
        EvaluateStatement eval = (EvaluateStatement) p1.getStatements().stream()
                .filter(s -> s instanceof EvaluateStatement)
                .findFirst().orElseThrow();
        assertEquals("WS-NUM", eval.getExpression());
        assertEquals(2, eval.getBranches().size());
        assertEquals("0", eval.getBranches().get(0).getCondition());
        assertEquals("OTHER", eval.getBranches().get(1).getCondition());

        // Control flow edges due to PERFORM and THRU
        ControlFlowGraph g = model.getControlFlowGraph();
        Map<String, Set<String>> adj = g.getAdjacency();
        assertTrue(adj.get("PARA-1").contains("PARA-2"));
        assertTrue(adj.get("PARA-2").contains("PARA-3"));

        // Arithmetic fallbacks
        String cobol2 = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. DEMO2.
                PROCEDURE DIVISION.
                MAIN.
                    COMPUTE BAD.
                    ADD X.
                    SUBTRACT X.
                    MULTIPLY X.
                    DIVIDE X.
                    READ.
                """;
        CobolIntermediateModel m2 = parser.parse(cobol2);
        CobolParagraph mp = m2.getEntryParagraph();
        // Ensure statements created even for malformed/degenerate forms
        assertTrue(mp.getStatements().stream().filter(s -> s instanceof ComputeStatement).count() >= 5);
        // Unknown file name results in UNKNOWN
        FileOperationStatement read = (FileOperationStatement) mp.getStatements().stream()
                .filter(s -> s instanceof FileOperationStatement)
                .findFirst().orElseThrow();
        assertEquals("UNKNOWN", read.getFileName());
    }

    @Test
    void parse_shouldExtract_entry_paragraphs_and_ignore_exit_program_body() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. DEMO3.
                PROCEDURE DIVISION.
                ENTRY "A" USING ARG1.
                    MOVE 'X' TO ARG1.
                    EXIT PROGRAM.
                    MOVE 'SHOULD-NOT-BE-HERE' TO ARG1.
                ENTRY "B".
                    MOVE 'Y' TO ARG1.
                """;
        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(cobol);
        assertTrue(model.getParagraphs().containsKey("A"));
        assertTrue(model.getParagraphs().containsKey("B"));
        CobolParagraph a = model.getParagraphs().get("A");
        assertEquals(1, a.getStatements().size());
        MoveStatement onlyMove = (MoveStatement) a.getStatements().get(0);
        assertEquals("'X'", onlyMove.getSource());
        assertEquals("ARG1", onlyMove.getTarget());
    }

    @Test
    void parse_shouldHandle_call_without_using_and_execSql_without_endExec() throws Exception {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. DEMO4.
                PROCEDURE DIVISION.
                MAIN.
                    CALL "NOUSING".
                    EXEC SQL SELECT 1 FROM T
                    .
                """;
        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(cobol);
        CobolParagraph p = model.getEntryParagraph();
        assertTrue(p.getStatements().stream().anyMatch(s -> s instanceof CallStatement));
        Db2Statement sql = (Db2Statement) p.getStatements().stream()
                .filter(s -> s instanceof Db2Statement).findFirst().orElseThrow();
        assertTrue(sql.getSql().toUpperCase().contains("SELECT 1 FROM T"));
    }

    @Test
    void parse_fromPath_shouldDefaultProgramId_whenMissing() throws Exception {
        String src = """
                PROCEDURE DIVISION.
                MAIN.
                    MOVE 'X' TO VAR.
                """;
        Path tmp = Files.createTempFile("cobol", ".cob");
        Files.writeString(tmp, src);
        try {
            SimpleCobolIrParser parser = new SimpleCobolIrParser();
            CobolIntermediateModel model = parser.parse(tmp);
            assertEquals("COBOLPROGRAM", model.getProgramId());
            assertEquals("MAIN", model.getEntryParagraph().getName());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void parse_shouldDeduplicate_workingStorage_items() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. DEDUP.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 DUP PIC 9(2).
                01 DUP PIC 9(3).
                PROCEDURE DIVISION.
                MAIN.
                    GOBACK.
                """;
        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(cobol);
        assertEquals(1, model.getDataItems().size());
        assertEquals("DUP", model.getDataItems().get(0).getName());
    }
}
