package org.shark.renovatio.shared.nql;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NqlQueryTest {

    @Test
    void getters_setters_and_constructors() {
        NqlQuery q = new NqlQuery();
        q.setType(NqlQuery.QueryType.FIND);
        q.setTarget("classes");
        q.setPredicate("name = 'X'");
        q.setScope("workspace");
        q.setReturnClause("name");
        q.setParameters(Map.of("k","v"));
        q.setOriginalQuery("FIND classes WHERE name = 'X'");
        q.setLanguage("java");

        assertEquals(NqlQuery.QueryType.FIND, q.getType());
        assertEquals("classes", q.getTarget());
        assertEquals("name = 'X'", q.getPredicate());
        assertEquals("workspace", q.getScope());
        assertEquals("name", q.getReturnClause());
        assertEquals("v", q.getParameters().get("k"));
        assertEquals("FIND classes WHERE name = 'X'", q.getOriginalQuery());
        assertEquals("java", q.getLanguage());

        NqlQuery q2 = new NqlQuery(NqlQuery.QueryType.APPLY, "files", "size > 0");
        assertEquals(NqlQuery.QueryType.APPLY, q2.getType());
        assertEquals("files", q2.getTarget());
        assertEquals("size > 0", q2.getPredicate());
    }
}

