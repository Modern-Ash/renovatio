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
}
