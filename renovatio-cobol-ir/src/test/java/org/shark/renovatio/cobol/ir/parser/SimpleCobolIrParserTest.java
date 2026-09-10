package org.shark.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.model.CobolParagraph;
import org.shark.renovatio.cobol.ir.model.IfStatement;
import org.shark.renovatio.cobol.ir.model.MoveStatement;

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
}
