package org.shark.renovatio.shared.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetricsResultTest {

    @SuppressWarnings("unchecked")
    @Test
    void setters_and_null_fallbacks_do_not_throw_and_preserve_data() throws Exception {
        MetricsResult r = new MetricsResult(true, "ok");
        // Exercise explicit setters and null fallbacks to cover branches
        r.setMetrics(Map.of("a", 1));
        r.setDetails(Map.of("k", "v"));

        Field fMetrics = MetricsResult.class.getDeclaredField("metrics");
        fMetrics.setAccessible(true);
        Map<String, Number> m = (Map<String, Number>) fMetrics.get(r);
        assertEquals(1, m.get("a"));

        Field fDetails = MetricsResult.class.getDeclaredField("details");
        fDetails.setAccessible(true);
        Map<String, Object> d = (Map<String, Object>) fDetails.get(r);
        assertEquals("v", d.get("k"));

        // Null fallbacks
        r.setMetrics(null);
        r.setDetails(null);
        m = (Map<String, Number>) fMetrics.get(r);
        d = (Map<String, Object>) fDetails.get(r);
        assertNotNull(m);
        assertNotNull(d);
    }
}
