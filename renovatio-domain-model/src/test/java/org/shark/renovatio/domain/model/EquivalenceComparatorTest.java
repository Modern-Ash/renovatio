package org.shark.renovatio.domain.model;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class EquivalenceComparatorTest {
    @Test void reportsOnlyObservableDifferences() {
        var result = EquivalenceComparator.compare(Map.of("amount", 10, "trace", "a"), Map.of("amount", 11, "trace", "b"), java.util.Set.of("trace"));
        assertFalse(result.equivalent());
        assertEquals("amount", result.divergences().iterator().next().field());
    }
}
