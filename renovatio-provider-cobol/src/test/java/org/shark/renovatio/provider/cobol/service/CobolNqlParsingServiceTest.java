package org.shark.renovatio.provider.cobol.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.nql.NqlParserService;
import org.shark.renovatio.shared.nql.NqlQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CobolNqlParsingServiceTest {

    private CobolNqlParsingService service;

    @BeforeEach
    void setUp() {
        service = new CobolNqlParsingService(new NqlParserService());
    }

    @Test
    void parsesValidNql() {
        NqlQuery query = service.parseNqlQuery("FIND programs WHERE name = 'A'");
        assertEquals(NqlQuery.QueryType.FIND, query.getType());
        assertEquals("programs", query.getTarget());
    }

    @Test
    void returnsFallbackForInvalidNql() {
        NqlQuery query = service.parseNqlQuery("INVALID");
        assertNull(query.getType());
        assertEquals("INVALID", query.getOriginalQuery());
    }
}
