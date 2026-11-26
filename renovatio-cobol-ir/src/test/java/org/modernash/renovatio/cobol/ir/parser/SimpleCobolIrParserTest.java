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
}
