package org.shark.renovatio.domain.model;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
class EquivalenceFixtureTest {
    @Test void comparesAReplayEnvelope() {
        var fixture = new EquivalenceFixture("case-1", Map.of("id", 1), Map.of("status", "OK"), Map.of("status", "OK"), null);
        assertTrue(fixture.compare().equivalent());
    }
}
