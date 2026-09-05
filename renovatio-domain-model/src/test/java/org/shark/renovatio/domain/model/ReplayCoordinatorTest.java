package org.shark.renovatio.domain.model;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
class ReplayCoordinatorTest {
 @Test void buildsComparableFixture() {
  ReplayRunner r = i -> new ReplayRunner.ReplayResult("OK", Map.of("v", 1), null, null, null);
  assertTrue(new ReplayCoordinator(r, r).run(new ReplayRunner.ReplayInput("x", Map.of()), null).compare().equivalent());
 }
}
