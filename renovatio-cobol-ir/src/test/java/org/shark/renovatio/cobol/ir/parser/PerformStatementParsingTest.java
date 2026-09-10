package org.shark.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.model.PerformStatement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformStatementParsingTest {

    private final SimpleCobolIrParser parser = new SimpleCobolIrParser();

    private List<PerformStatement> performStatements(String procedure) {
        CobolIntermediateModel model = parser.parse(procedure);
        return model.getParagraphs().values().stream()
                .flatMap(p -> p.statements().stream())
                .filter(PerformStatement.class::isInstance)
                .map(PerformStatement.class::cast)
                .toList();
    }

    private static String program(String statements) {
        return """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. PERFORM-TEST.
                PROCEDURE DIVISION.
                MAIN-PARA.
                %s
                    GOBACK.
                100-ADD.
                    CONTINUE.
                200-DISPLAY.
                    CONTINUE.
                """.formatted(statements);
    }

    @Test
    void parsesPlainPerform() {
        List<PerformStatement> statements = performStatements(program("    PERFORM 100-ADD."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertEquals("100-ADD", s.paragraph());
        assertNull(s.throughParagraph());
        assertFalse(s.hasVarying());
        assertFalse(s.hasUntil());
        assertFalse(s.hasTimes());
    }

    @Test
    void parsesDigitPrefixedTargetParagraphsIntoModel() {
        CobolIntermediateModel model = parser.parse(program("    PERFORM 100-ADD THRU 200-DISPLAY."));
        assertTrue(model.findParagraph("100-ADD").isPresent(), "100-ADD missing from model");
        assertTrue(model.findParagraph("200-DISPLAY").isPresent(), "200-DISPLAY missing from model");
        assertTrue(model.findParagraphLineRange("100-ADD").isPresent(), "100-ADD line range missing");
        assertTrue(model.findParagraphLineRange("200-DISPLAY").isPresent(), "200-DISPLAY line range missing");
    }

    @Test
    void parsesPerformThru() {
        List<PerformStatement> statements = performStatements(program("    PERFORM 100-ADD THRU 200-DISPLAY."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertEquals("100-ADD", s.paragraph());
        assertEquals("200-DISPLAY", s.throughParagraph());
    }

    @Test
    void parsesPerformTimes() {
        List<PerformStatement> statements = performStatements(program("    PERFORM 100-ADD 3 TIMES."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.hasTimes());
        assertEquals(3, s.timesCount());
        assertEquals("100-ADD", s.paragraph());
    }

    @Test
    void parsesPerformThruTimes() {
        List<PerformStatement> statements = performStatements(program("    PERFORM 100-ADD THRU 200-DISPLAY 2 TIMES."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertEquals("100-ADD", s.paragraph());
        assertEquals("200-DISPLAY", s.throughParagraph());
        assertEquals(2, s.timesCount());
    }

    @Test
    void parsesPerformUntil() {
        List<PerformStatement> statements = performStatements(
                program("    PERFORM 100-ADD UNTIL WS-COUNT = 5."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.hasUntil());
        assertEquals("WS-COUNT = 5", s.untilCondition());
        assertEquals("100-ADD", s.paragraph());
    }

    @Test
    void parsesPerformVaryingUntil() {
        List<PerformStatement> statements = performStatements(
                program("    PERFORM 100-ADD VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 10."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.hasVarying());
        assertEquals("WS-IDX", s.varyingVariable());
        assertEquals("1", s.varyingFrom());
        assertEquals("1", s.varyingBy());
        assertEquals("WS-IDX > 10", s.untilCondition());
    }

    @Test
    void parsesPerformVaryingWithoutBy() {
        List<PerformStatement> statements = performStatements(
                program("    PERFORM 100-ADD VARYING WS-IDX FROM 1 UNTIL WS-IDX > 5."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertEquals("WS-IDX", s.varyingVariable());
        assertEquals("1", s.varyingFrom());
        assertNull(s.varyingBy());
        assertEquals("WS-IDX > 5", s.untilCondition());
    }

    @Test
    void parsesPerformVaryingWithAfterAxes() {
        List<PerformStatement> statements = performStatements(program(
                "    PERFORM 100-ADD VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 2"
                        + " AFTER WS-J FROM 1 BY 1 UNTIL WS-J > 3"
                        + " AFTER WS-K FROM 0 BY 2 UNTIL WS-K > 4."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertEquals("WS-I", s.varyingVariable());
        assertEquals("WS-I > 2", s.untilCondition());
        assertEquals(2, s.varyingAfter().size());
        PerformStatement.VaryingAxis j = s.varyingAfter().get(0);
        assertEquals("WS-J", j.variable());
        assertEquals("1", j.from());
        assertEquals("1", j.by());
        assertEquals("WS-J > 3", j.until());
        PerformStatement.VaryingAxis k = s.varyingAfter().get(1);
        assertEquals("WS-K", k.variable());
        assertEquals("0", k.from());
        assertEquals("2", k.by());
        assertEquals("WS-K > 4", k.until());
    }

    @Test
    void parsesInlineVaryingWithAfterAxis() {
        List<PerformStatement> statements = performStatements(program(
                "    PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 2 AFTER WS-J FROM 1 BY 1 UNTIL WS-J > 3\n"
                        + "        MOVE WS-J TO WS-TRACE-NUM\n"
                        + "    END-PERFORM."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.isInline());
        assertEquals("WS-I", s.varyingVariable());
        assertEquals(1, s.varyingAfter().size());
        assertEquals("WS-J", s.varyingAfter().get(0).variable());
    }

    @Test
    void parsesPerformThruVaryingUntil() {
        List<PerformStatement> statements = performStatements(
                program("    PERFORM 100-ADD THRU 200-DISPLAY VARYING WS-IDX FROM 1 BY 2 UNTIL WS-IDX > 10."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertEquals("100-ADD", s.paragraph());
        assertEquals("200-DISPLAY", s.throughParagraph());
        assertEquals("WS-IDX", s.varyingVariable());
        assertEquals("1", s.varyingFrom());
        assertEquals("2", s.varyingBy());
        assertEquals("WS-IDX > 10", s.untilCondition());
    }

    @Test
    void parsesMultiLineThruContinuation() {
        String procedure = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. PERFORM-TEST.
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM 100-ADD THRU
                            200-DISPLAY.
                    GOBACK.
                100-ADD.
                    CONTINUE.
                200-DISPLAY.
                    CONTINUE.
                """;
        List<PerformStatement> statements = performStatements(procedure);
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertEquals("100-ADD", s.paragraph());
        assertEquals("200-DISPLAY", s.throughParagraph());
        assertFalse(s.isInline());
    }

    @Test
    void parsesInlineUntilBlock() {
        List<PerformStatement> statements = performStatements(program("""
                PERFORM UNTIL WS-DONE = 'Y'
                    COMPUTE WS-COUNT = WS-COUNT + 1
                END-PERFORM."""));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.isInline());
        assertTrue(s.hasUntil());
        assertEquals("WS-DONE = 'Y'", s.untilCondition());
        assertEquals(1, s.inlineBody().size());
    }

    @Test
    void parsesInlineVaryingBlock() {
        List<PerformStatement> statements = performStatements(program("""
                PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 10
                    PERFORM 100-ADD
                END-PERFORM."""));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.isInline());
        assertTrue(s.hasVarying());
        assertEquals("WS-IDX", s.varyingVariable());
        assertEquals("WS-IDX > 10", s.untilCondition());
        assertEquals(1, s.inlineBody().size());
    }

    @Test
    void parsesInlineTimesBlock() {
        List<PerformStatement> statements = performStatements(program("""
                PERFORM 3 TIMES
                    COMPUTE WS-ACC = WS-ACC + 1
                END-PERFORM."""));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.isInline());
        assertTrue(s.hasTimes());
        assertEquals(3, s.timesCount());
        assertEquals(1, s.inlineBody().size());
    }

    @Test
    void parsesNestedInlinePerformBlocks() {
        List<PerformStatement> statements = performStatements(program("""
                PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 10
                    PERFORM 3 TIMES
                        COMPUTE WS-ACC = WS-ACC + WS-IDX
                    END-PERFORM
                    PERFORM 100-ADD
                END-PERFORM."""));
        assertEquals(1, statements.size());
        PerformStatement outer = statements.get(0);
        assertTrue(outer.isInline());
        assertEquals(2, outer.inlineBody().size());
        PerformStatement inner = (PerformStatement) outer.inlineBody().get(0);
        assertTrue(inner.isInline());
        assertTrue(inner.hasTimes());
        assertEquals(3, inner.timesCount());
    }

    @Test
    void parsesPerformWithTestAfter() {
        List<PerformStatement> statements = performStatements(
                program("    PERFORM 100-ADD WITH TEST AFTER UNTIL WS-COUNT = 5."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.testAfter());
        assertTrue(s.hasUntil());
        assertEquals("WS-COUNT = 5", s.untilCondition());
        assertEquals("100-ADD", s.paragraph());
    }

    @Test
    void parsesPerformWithTestBefore() {
        List<PerformStatement> statements = performStatements(
                program("    PERFORM 100-ADD WITH TEST BEFORE UNTIL WS-COUNT = 5."));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertFalse(s.testAfter());
        assertEquals("WS-COUNT = 5", s.untilCondition());
    }

    @Test
    void parsesInlinePerformWithTestAfterUntil() {
        List<PerformStatement> statements = performStatements(program("""
                PERFORM WITH TEST AFTER UNTIL WS-DONE = 'Y'
                    COMPUTE WS-COUNT = WS-COUNT + 1
                END-PERFORM."""));
        assertEquals(1, statements.size());
        PerformStatement s = statements.get(0);
        assertTrue(s.isInline());
        assertTrue(s.testAfter());
        assertEquals("WS-DONE = 'Y'", s.untilCondition());
        assertEquals(1, s.inlineBody().size());
    }
}