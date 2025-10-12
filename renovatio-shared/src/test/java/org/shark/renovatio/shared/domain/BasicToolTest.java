package org.shark.renovatio.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BasicToolTest {

    @Test
    void getters_setters_and_null_fallbacks() {
        BasicTool t = new BasicTool();
        assertNotNull(t.getInputSchema());
        assertNotNull(t.getMetadata());

        t.setName("tool");
        t.setDescription("desc");
        t.setInputSchema(null);
        t.setMetadata(null);

        assertEquals("tool", t.getName());
        assertEquals("desc", t.getDescription());
        assertNotNull(t.getInputSchema());
        assertNotNull(t.getMetadata());

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        t.setInputSchema(schema);
        assertEquals("object", t.getInputSchema().get("type"));
    }

    @Test
    void constructors_initialize_fields() {
        BasicTool t1 = new BasicTool("n", "d");
        assertEquals("n", t1.getName());
        assertEquals("d", t1.getDescription());

        BasicTool t2 = new BasicTool("n2", "d2", Map.of("k", 1));
        assertEquals(1, t2.getInputSchema().get("k"));
    }
}

