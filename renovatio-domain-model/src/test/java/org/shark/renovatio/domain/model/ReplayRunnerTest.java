package org.shark.renovatio.domain.model;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
class ReplayRunnerTest {
    @Test void preservesCommonReplayEnvelope() {
        ReplayRunner runner = input -> new ReplayRunner.ReplayResult("SUCCESS", Map.of("id", input.caseId()), null, null, null);
        assertEquals("case-1", runner.run(new ReplayRunner.ReplayInput("case-1", Map.of())).output().get("id"));
    }
}
