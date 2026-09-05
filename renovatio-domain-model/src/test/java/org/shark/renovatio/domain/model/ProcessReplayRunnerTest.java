package org.shark.renovatio.domain.model;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
class ProcessReplayRunnerTest {
 @Test void capturesProcessOutput() {
  var result = new ProcessReplayRunner(List.of("sh", "-c", "printf replay-ok"), null).run(new ReplayRunner.ReplayInput("x", null));
  assertEquals("SUCCESS", result.status());
  assertEquals("replay-ok", result.output().get("stdout"));
 }
}
