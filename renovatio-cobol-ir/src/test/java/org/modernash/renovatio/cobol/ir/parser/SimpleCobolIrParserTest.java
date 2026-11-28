package org.modernash.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.model.CobolParagraph;
import org.modernash.renovatio.cobol.ir.model.IfStatement;
import org.modernash.renovatio.cobol.ir.model.MoveStatement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleCobolIrParserTest {

    private static final String COBOL_SAMPLE = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE1.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            01 CUSTOMER-RATING PIC 9(2).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'JOHN' TO CUSTOMER-NAME.
                IF CUSTOMER-RATING > 80
                    MOVE 'VIP' TO CUSTOMER-NAME
                ELSE
                    MOVE 'STANDARD' TO CUSTOMER-NAME
                END-IF.
                GOBACK.
            END-PARA.
            """;

    @Test
    void shouldParseProgramIdDataItemsAndIfStructure() {
        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(COBOL_SAMPLE);

        assertEquals("SAMPLE1", model.getProgramId());
        assertEquals(2, model.getDataItems().size());

        CobolParagraph main = model.getEntryParagraph();
        assertEquals("MAIN-PARA", main.name());
        assertFalse(main.statements().isEmpty());

        MoveStatement move = (MoveStatement) main.statements().get(0);
        assertEquals("'JOHN'", move.source());
        assertEquals("CUSTOMER-NAME", move.target());

        IfStatement ifStatement = (IfStatement) main.statements().stream()
                .filter(stmt -> stmt instanceof IfStatement)
                .findFirst()
                .orElseThrow();
        assertEquals("CUSTOMER-RATING > 80", ifStatement.condition());
        List<CobolParagraph> paragraphs = model.getParagraphs().values().stream().toList();
        assertEquals(2, paragraphs.size());
    }

    @Test
    void shouldKeepUsageClauseOutOfPictureWhenValueClauseIsPresent() {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. TYPES01.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 WS-COUNT PIC S9(4) COMP VALUE 0.
            01 WS-AMOUNT PIC S9(8)V99 COMP-3 VALUE 0.
            PROCEDURE DIVISION.
            MAIN-PARA.
                GOBACK.
            """);

        assertEquals("S9(4) COMP", model.getDataItems().get(0).picture());
        assertEquals("Integer", model.getDataItems().get(0).javaType());
        assertEquals("S9(8)V99 COMP-3", model.getDataItems().get(1).picture());
        assertEquals("BigDecimal", model.getDataItems().get(1).javaType());
    }

    @Test
    void shouldKeepValueAndOtherClausesOutOfDisplayPicture() {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. TYPES02.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 WS-FLAG PIC 9   VALUE 0.
            01 WS-COUNTER PIC 999 VALUE ZEROS.
            01 WS-RATE PIC S9(3)V99 VALUE -1.
            01 WS-NAME PIC X(10) VALUE SPACES.
            01 WS-TOTAL PIC 9(5) USAGE IS COMP-3 VALUE 0.
            PROCEDURE DIVISION.
            MAIN-PARA.
                GOBACK.
            """);

        assertEquals("9", model.getDataItems().get(0).picture());
        assertEquals("Integer", model.getDataItems().get(0).javaType());
        assertEquals("999", model.getDataItems().get(1).picture());
        assertEquals("Integer", model.getDataItems().get(1).javaType());
        assertEquals("S9(3)V99", model.getDataItems().get(2).picture());
        assertEquals("BigDecimal", model.getDataItems().get(2).javaType());
        assertEquals("X(10)", model.getDataItems().get(3).picture());
        assertEquals("String", model.getDataItems().get(3).javaType());
        assertEquals("9(5) COMP-3", model.getDataItems().get(4).picture());
        assertEquals("Integer", model.getDataItems().get(4).javaType());
        assertTrue(model.getDiagnostics().isEmpty(), () -> String.valueOf(model.getDiagnostics()));
    }

    @Test
    void shouldIgnoreFixedFormatSequenceAreaInColumns73To80() {
        String display = "           DISPLAY 'TODAY :' WS-DATE";
        String line = display + " ".repeat(72 - display.length()) + "01790043";
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SEQ01.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-DATE PIC X(8).",
                "       PROCEDURE DIVISION.",
                "       MAIN-PARA.",
                line,
                "           GOBACK."));

        var statement = (org.modernash.renovatio.cobol.ir.model.SimpleStatement)
                model.getEntryParagraph().statements().get(0);
        assertEquals("'TODAY :' WS-DATE", statement.text());
    }

    private static CobolIntermediateModel procedure(String... body) {
        StringBuilder source = new StringBuilder("""
            IDENTIFICATION DIVISION.
            PROGRAM-ID. IFNEST.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 WS-A PIC X.
            01 WS-B PIC X.
            01 WS-C PIC 9.
            PROCEDURE DIVISION.
            MAIN-PARA.
            """);
        for (String line : body) {
            source.append(line).append('\n');
        }
        return new SimpleCobolIrParser().parse(source.toString());
    }

    @Test
    void shouldKeepNestedIfElseInsideTheOuterThenBranch() {
        CobolIntermediateModel model = procedure(
                "    IF WS-A = 'Y'",
                "        IF WS-B = 'N'",
                "            MOVE 1 TO WS-C",
                "        ELSE",
                "            MOVE 2 TO WS-C",
                "        END-IF",
                "        MOVE 3 TO WS-C",
                "    ELSE",
                "        MOVE 4 TO WS-C",
                "    END-IF",
                "    GOBACK.");

        List<org.modernash.renovatio.cobol.ir.model.CobolStatement> statements =
                model.getEntryParagraph().statements();
        assertEquals(2, statements.size(), () -> String.valueOf(statements));
        IfStatement outer = (IfStatement) statements.get(0);
        assertEquals(2, outer.thenStatements().size());
        IfStatement inner = (IfStatement) outer.thenStatements().get(0);
        assertEquals("2", ((MoveStatement) inner.elseStatements().get(0)).source());
        assertEquals("3", ((MoveStatement) outer.thenStatements().get(1)).source());
        assertEquals("4", ((MoveStatement) outer.elseStatements().get(0)).source());
    }

    @Test
    void shouldJoinIfConditionContinuedOnFollowingLines() {
        CobolIntermediateModel model = procedure(
                "    IF WS-A NOT = '0'",
                "                 AND WS-A NOT = '1'",
                "        MOVE 9 TO WS-C",
                "    END-IF",
                "    GOBACK.");

        IfStatement statement = (IfStatement) model.getEntryParagraph().statements().get(0);
        assertTrue(statement.condition().contains("AND"), statement.condition());
        assertEquals(1, statement.thenStatements().size());
    }

    @Test
    void shouldEndIfWithoutEndIfAtTheSentencePeriod() {
        CobolIntermediateModel model = procedure(
                "    IF WS-A = 'Y'",
                "        MOVE 1 TO WS-C.",
                "    MOVE 2 TO WS-C.",
                "    GOBACK.");

        List<org.modernash.renovatio.cobol.ir.model.CobolStatement> statements =
                model.getEntryParagraph().statements();
        assertEquals(3, statements.size(), () -> String.valueOf(statements));
        assertEquals(1, ((IfStatement) statements.get(0)).thenStatements().size());
    }

    @Test
    void shouldNotTreatProcedureDivisionUsingContinuationAsParagraph() {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. USING01.",
                "       DATA DIVISION.",
                "       LINKAGE SECTION.",
                "       01 IO-PCB-MASK PIC X(4).",
                "       01 PGM-PCB-MASK PIC X(4).",
                "       PROCEDURE DIVISION                  USING IO-PCB-MASK",
                "                                                 PGM-PCB-MASK.",
                "       MAIN-PARA.",
                "           GOBACK."));

        assertEquals("MAIN-PARA", model.getEntryParagraph().name());
        assertTrue(model.findParagraph("PGM-PCB-MASK").isEmpty());
        assertTrue(model.findParagraphLineRange("PGM-PCB-MASK").isEmpty());
    }

    @Test
    void shouldKeepDecimalPointOfNumericLiterals() {
        CobolIntermediateModel model = procedure(
                "    MOVE 2525.00 TO WS-C.",
                "    IF WS-C > 1.5 THEN",
                "        MOVE -1025.50 TO WS-C",
                "    END-IF",
                "    GOBACK.");

        List<org.modernash.renovatio.cobol.ir.model.CobolStatement> statements =
                model.getEntryParagraph().statements();
        assertEquals("2525.00", ((MoveStatement) statements.get(0)).source());
        IfStatement branch = (IfStatement) statements.get(1);
        assertEquals("WS-C > 1.5", branch.condition());
        assertEquals("-1025.50", ((MoveStatement) branch.thenStatements().get(0)).source());
    }

    @Test
    void shouldKeepStatementsBeforeTheFirstParagraphAsTheEntryParagraph() {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. MAINLINE.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01 WS-C PIC 9.",
                "       PROCEDURE DIVISION.",
                "           PERFORM 1000-WORK.",
                "           GOBACK.",
                "       1000-WORK.",
                "           MOVE 1 TO WS-C.",
                "           EXIT."));

        assertEquals(SimpleCobolIrParser.IMPLICIT_MAIN_PARAGRAPH, model.getEntryParagraph().name());
        assertEquals(2, model.getEntryParagraph().statements().size());
        assertTrue(model.findParagraph("1000-WORK").isPresent());
    }

    @Test
    void shouldIgnoreSequenceNumbersInColumns1To6() {
        String[] body = {
                "000100 IDENTIFICATION DIVISION.",
                "000200 PROGRAM-ID. SEQ06.",
                "000300 DATA DIVISION.",
                "000400 WORKING-STORAGE SECTION.",
                "000500 01 WS-C PIC 9.",
                "000600 PROCEDURE DIVISION.",
                "000700 0000-MAIN.",
                "000800     MOVE 1 TO WS-C.",
                "000900     GOBACK."};
        StringBuilder source = new StringBuilder();
        for (String line : body) {
            source.append(line).append(" ".repeat(80 - line.length())).append('\n');
        }
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(source.toString());

        assertEquals("0000-MAIN", model.getEntryParagraph().name());
        assertEquals("1", ((MoveStatement) model.getEntryParagraph().statements().get(0)).source());
    }

    @Test
    void shouldParseArithmeticVerbFormsIntoOneComputePerReceivingItem() {
        CobolIntermediateModel model = procedure(
                "    ADD 8 TO ZERO GIVING WS-C",
                "    SUBTRACT WS-C FROM WS-C",
                "    ADD 1 2 TO WS-A WS-B",
                "    MULTIPLY 3 BY WS-C GIVING WS-A",
                "    DIVIDE 2 INTO WS-C",
                "    DIVIDE WS-C BY 4 GIVING WS-B",
                "    ADD 1 TO WS-C ROUNDED",
                "    GOBACK.");

        List<org.modernash.renovatio.cobol.ir.model.CobolStatement> statements =
                model.getEntryParagraph().statements();
        java.util.function.Function<Integer, org.modernash.renovatio.cobol.ir.model.ComputeStatement> compute =
                index -> (org.modernash.renovatio.cobol.ir.model.ComputeStatement) statements.get(index);
        assertEquals("WS-C", compute.apply(0).target());
        assertEquals("8 + 0", compute.apply(0).expression());
        assertEquals("WS-C - WS-C", compute.apply(1).expression());
        assertEquals("WS-A + 1 + 2", compute.apply(2).expression());
        assertEquals("WS-B + 1 + 2", compute.apply(3).expression());
        assertEquals("3 * WS-C", compute.apply(4).expression());
        assertEquals("WS-C / 2", compute.apply(5).expression());
        assertEquals("WS-C / 4", compute.apply(6).expression());
        assertEquals(org.modernash.renovatio.cobol.ir.model.SimpleStatement.Kind.UNTRANSLATED,
                ((org.modernash.renovatio.cobol.ir.model.SimpleStatement) statements.get(7)).kind());
    }

    @Test
    void shouldKeepNestedEvaluateWhensAndJoinStackedWhens() {
        CobolIntermediateModel model = procedure(
                "    EVALUATE WS-C",
                "        WHEN 1",
                "        WHEN 2",
                "            EVALUATE WS-A",
                "                WHEN 'X'",
                "                    MOVE 3 TO WS-C",
                "                WHEN OTHER",
                "                    MOVE 4 TO WS-C",
                "            END-EVALUATE",
                "        WHEN OTHER",
                "            MOVE 5 TO WS-C",
                "    END-EVALUATE",
                "    GOBACK.");

        org.modernash.renovatio.cobol.ir.model.EvaluateStatement outer =
                (org.modernash.renovatio.cobol.ir.model.EvaluateStatement) model.getEntryParagraph().statements().get(0);
        assertEquals(2, outer.branches().size(), () -> String.valueOf(outer.branches()));
        assertEquals("1 OR 2", outer.branches().get(0).condition());
        org.modernash.renovatio.cobol.ir.model.EvaluateStatement inner =
                (org.modernash.renovatio.cobol.ir.model.EvaluateStatement) outer.branches().get(0).statements().get(0);
        assertEquals(2, inner.branches().size());
        assertEquals("OTHER", outer.branches().get(1).condition());
    }

    @Test
    void expandsQuotedCopyAndCopyReplacingInTheProcedureDivision(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        java.nio.file.Files.writeString(dir.resolve("SETATTR.cpy"), String.join("\n",
                "           IF FLG-(TESTVAR1)-BAD",
                "               MOVE 'X' TO (SCRNVAR2)O",
                "           END-IF"));
        java.nio.file.Files.writeString(dir.resolve("TAILPARA.cpy"), String.join("\n",
                "       TAIL-PARA.",
                "           MOVE 'T' TO WS-OUT."));
        java.nio.file.Files.writeString(dir.resolve("PROG.cbl"), String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. PROG.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01  WS-OUT PIC X.",
                "       PROCEDURE DIVISION.",
                "       MAIN-PARA.",
                "           COPY SETATTR REPLACING",
                "             ==(TESTVAR1)== BY ==ACCT==",
                "             ==(SCRNVAR2)== BY ==ACCTS== .",
                "           GOBACK.",
                "       COPY 'TAILPARA'",
                "           ."));

        CobolIntermediateModel model = new SimpleCobolIrParser().parse(dir.resolve("PROG.cbl"));

        IfStatement copied = (IfStatement) model.getParagraphs().get("MAIN-PARA").statements().get(0);
        assertTrue(copied.condition().toString().contains("FLG-ACCT-BAD"), copied.condition().toString());
        assertEquals("ACCTSO", ((MoveStatement) copied.thenStatements().get(0)).target());
        assertNotNull(model.getParagraphs().get("TAIL-PARA"), "paragraph from the quoted COPY");
    }

    @Test
    void joinsAMoveWhoseReceivingPartIsOnTheNextLine() {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. SPLITMV.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01  WS-DESC PIC X(30).",
                "       PROCEDURE DIVISION.",
                "       MAIN-PARA.",
                "           MOVE 'ACCOUNT RECORD NOT FOUND'",
                "             TO WS-DESC",
                "           MOVE 'Select a report to print' TO",
                "                WS-DESC",
                "           GOBACK."));

        List<?> statements = model.getParagraphs().get("MAIN-PARA").statements();
        assertEquals("WS-DESC", ((MoveStatement) statements.get(0)).target());
        assertEquals("WS-DESC", ((MoveStatement) statements.get(1)).target());
        assertEquals(3, statements.size());
    }

    @Test
    void readsBmsSymbolicMapEntriesWithUsageBeforePictureAndPictureKeyword() {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(String.join("\n",
                "       IDENTIFICATION DIVISION.",
                "       PROGRAM-ID. BMSMAP.",
                "       DATA DIVISION.",
                "       WORKING-STORAGE SECTION.",
                "       01  USRMAPI.",
                "           02  USRIDL    COMP  PIC  S9(4).",
                "           02  USRIDF    PICTURE X.",
                "           02  USRIDI  PIC X(8).",
                "       PROCEDURE DIVISION.",
                "       MAIN-PARA.",
                "           GOBACK."));

        var items = model.getDataItems().stream()
                .collect(java.util.stream.Collectors.toMap(item -> item.getName(), item -> item, (a, b) -> a));
        assertEquals("COMP", items.get("USRIDL").getPicType().usage().name());
        assertEquals("String", items.get("USRIDF").getJavaType());
        assertNotNull(items.get("USRIDI"));
    }
}
