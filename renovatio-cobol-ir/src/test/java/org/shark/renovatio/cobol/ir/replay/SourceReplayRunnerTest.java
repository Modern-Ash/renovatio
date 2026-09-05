package org.shark.renovatio.cobol.ir.replay;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceReplayRunnerTest {
    @Test void replaysMoveAndComputeFromSource() {
        String cobol = "IDENTIFICATION DIVISION. PROGRAM-ID. DEMO.\n" +
                "DATA DIVISION. WORKING-STORAGE SECTION.\n01 A PIC 9(3).\n01 B PIC 9(3).\n" +
                "PROCEDURE DIVISION.\nMAIN.\n    MOVE 2 TO A.\n    COMPUTE B = A + 3.\n    GOBACK.";
        var result = new SourceReplayRunner(cobol).run(new org.shark.renovatio.domain.model.ReplayRunner.ReplayInput("case-1", Map.of()));
        assertEquals("SUCCESS", result.status());
        assertEquals(2d, result.output().get("A"));
        assertEquals(5d, result.output().get("B"));
    }

    @Test void replaysIfElseFromSource() {
        String cobol = "IDENTIFICATION DIVISION. PROGRAM-ID. DEMO.\nDATA DIVISION. WORKING-STORAGE SECTION.\n01 A PIC 9(3).\n01 B PIC X(3).\nPROCEDURE DIVISION.\nMAIN.\n MOVE 2 TO A.\n IF A = 2\n   MOVE 'YES' TO B\n ELSE\n   MOVE 'NO' TO B\n END-IF.\n GOBACK.";
        var result = new SourceReplayRunner(cobol).run(new org.shark.renovatio.domain.model.ReplayRunner.ReplayInput("case-2", Map.of()));
        assertEquals("SUCCESS", result.status());
        assertEquals("YES", result.output().get("B"));
    }

    @Test void replaysEvaluateWhenOtherFromSource() {
        String cobol = "IDENTIFICATION DIVISION. PROGRAM-ID. DEMO.\nDATA DIVISION. WORKING-STORAGE SECTION.\n01 A PIC 9(3).\n01 B PIC X(3).\nPROCEDURE DIVISION.\nMAIN.\n MOVE 2 TO A.\n EVALUATE A\n   WHEN 1\n     MOVE 'ONE' TO B\n   WHEN 2\n     MOVE 'TWO' TO B\n   WHEN OTHER\n     MOVE 'OTH' TO B\n END-EVALUATE.\n GOBACK.";
        var result = new SourceReplayRunner(cobol).run(new org.shark.renovatio.domain.model.ReplayRunner.ReplayInput("case-3", Map.of()));
        assertEquals("SUCCESS", result.status());
        assertEquals("TWO", result.output().get("B"));
    }

    @Test void replaysCobolArithmeticVerbs() {
        String cobol = "IDENTIFICATION DIVISION. PROGRAM-ID. DEMO.\nDATA DIVISION. WORKING-STORAGE SECTION.\n01 A PIC 9(3).\nPROCEDURE DIVISION.\nMAIN.\n MOVE 10 TO A.\n ADD 2 TO A.\n SUBTRACT 1 FROM A.\n MULTIPLY 2 BY A.\n DIVIDE A BY 3 GIVING A.\n GOBACK.";
        var result = new SourceReplayRunner(cobol).run(new org.shark.renovatio.domain.model.ReplayRunner.ReplayInput("case-4", Map.of()));
        assertEquals("SUCCESS", result.status());
        assertEquals(7.333333333333333d, result.output().get("A"));
    }

    @Test void replaysSequentialReadFromInMemoryFile() {
        String cobol = "IDENTIFICATION DIVISION. PROGRAM-ID. DEMO.\nDATA DIVISION. WORKING-STORAGE SECTION.\n01 A PIC X(3).\nPROCEDURE DIVISION.\nMAIN.\n READ INPUT-FILE.\n READ INPUT-FILE.\n GOBACK.";
        var source = new org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser().parse(cobol);
        var runner = new SourceReplayRunner(source, Map.of("INPUT-FILE", java.util.List.of(Map.of("A", "ABC"), Map.of("A", "XYZ"))));
        var result = runner.run(new org.shark.renovatio.domain.model.ReplayRunner.ReplayInput("case-5", Map.of()));
        assertEquals("SUCCESS", result.status());
        assertEquals("XYZ", result.output().get("A"));
    }

    @Test void replaysDb2ResponseDeterministically() {
        String cobol = "IDENTIFICATION DIVISION. PROGRAM-ID. DEMO.\nDATA DIVISION. WORKING-STORAGE SECTION.\n01 A PIC X(3).\nPROCEDURE DIVISION.\nMAIN.\n EXEC SQL\n SELECT NAME INTO :A FROM CUSTOMER\n END-EXEC.\n GOBACK.";
        var source = new org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser().parse(cobol);
        var runner = new SourceReplayRunner(source, Map.of(), Map.of("SELECT NAME INTO :A FROM CUSTOMER", Map.of("A", "ACME")));
        var result = runner.run(new org.shark.renovatio.domain.model.ReplayRunner.ReplayInput("case-6", Map.of()));
        assertEquals("SUCCESS", result.status());
        assertEquals("ACME", result.output().get("A"));
        assertEquals(0, result.output().get("SQLCODE"));
    }
}
