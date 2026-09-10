package org.shark.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.model.CobolStatement;
import org.shark.renovatio.cobol.ir.model.SimpleStatement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** #206 — DISPLAY / CONTINUE / GOBACK / STOP RUN parsing and the "no silent drop" fallback. */
class SimpleStatementParsingTest {

    private List<CobolStatement> statementsOf(String procedureBody) {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SIMPLE.
                PROCEDURE DIVISION.
                MAIN-PARA.
                """ + procedureBody + "\n";
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(cobol);
        return model.getEntryParagraph().statements();
    }

    private SimpleStatement single(String procedureBody) {
        List<CobolStatement> statements = statementsOf(procedureBody);
        assertEquals(1, statements.size(), () -> "expected one statement, got " + statements);
        assertTrue(statements.get(0) instanceof SimpleStatement, () -> "not a SimpleStatement: " + statements.get(0));
        return (SimpleStatement) statements.get(0);
    }

    @Test
    void parsesDisplayWithLiteralAndIdentifier() {
        SimpleStatement display = single("    DISPLAY 'HELLO ' WS-NAME.");
        assertEquals(SimpleStatement.Kind.DISPLAY, display.kind());
        assertEquals("'HELLO ' WS-NAME", display.text());
    }

    @Test
    void parsesDisplayWithNoOperands() {
        SimpleStatement display = single("    DISPLAY.");
        assertEquals(SimpleStatement.Kind.DISPLAY, display.kind());
        assertEquals("", display.text());
    }

    @Test
    void parsesContinueGobackAndStop() {
        assertEquals(SimpleStatement.Kind.CONTINUE, single("    CONTINUE.").kind());
        assertEquals(SimpleStatement.Kind.GOBACK, single("    GOBACK.").kind());
        assertEquals(SimpleStatement.Kind.STOP_RUN, single("    STOP RUN.").kind());
        assertEquals(SimpleStatement.Kind.STOP_RUN, single("    STOP.").kind());
    }

    @Test
    void capturesUnrecognisedStatementInsteadOfDroppingIt() {
        SimpleStatement untranslated = single("    INSPECT WS-COUNTERS TALLYING WS-TOTAL FOR ALL 'X'.");
        assertEquals(SimpleStatement.Kind.UNTRANSLATED, untranslated.kind());
        assertEquals("INSPECT WS-COUNTERS TALLYING WS-TOTAL FOR ALL 'X'", untranslated.text());
    }

    @Test
    void treatsStopWithLiteralAsUntranslated() {
        assertEquals(SimpleStatement.Kind.UNTRANSLATED, single("    STOP 'PAUSE'.").kind());
    }

    @Test
    void ignoresStructuralNoiseLines() {
        List<CobolStatement> statements = statementsOf("""
                    MOVE 1 TO WS-A.
                    EXIT.
                    END-PERFORM.
                    NEXT SENTENCE.
                """);
        // Only the MOVE survives; EXIT / END-PERFORM / NEXT SENTENCE are structural noise.
        assertEquals(1, statements.size(), () -> statements.toString());
    }

    @Test
    void simpleStatementNormalisesNullText() {
        SimpleStatement statement = new SimpleStatement(SimpleStatement.Kind.CONTINUE, null);
        assertEquals("", statement.text());
    }
}
