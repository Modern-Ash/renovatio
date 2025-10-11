package org.shark.renovatio.shared.nql;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NqlCompileResultTest {

    @Test
    void full_bean_contract() {
        NqlCompileResult r = new NqlCompileResult();
        r.setSuccess(true);
        NqlQuery q = new NqlQuery();
        q.setOriginalQuery("Q");
        r.setQuery(q);
        r.setReasoning("why");
        r.setErrors(List.of("e1"));
        r.setMetadata(Map.of("k","v"));

        assertTrue(r.isSuccess());
        assertEquals("Q", r.getQuery().getOriginalQuery());
        assertEquals("why", r.getReasoning());
        assertEquals(1, r.getErrors().size());
        assertEquals("v", r.getMetadata().get("k"));
    }

    @Test
    void convenience_constructor() {
        NqlQuery q = new NqlQuery();
        NqlCompileResult r = new NqlCompileResult(false, q, "because");
        assertFalse(r.isSuccess());
        assertSame(q, r.getQuery());
        assertEquals("because", r.getReasoning());
    }
}

