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
}
