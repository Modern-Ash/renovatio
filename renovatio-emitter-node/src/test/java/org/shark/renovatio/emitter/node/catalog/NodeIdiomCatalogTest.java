package org.shark.renovatio.emitter.node.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeIdiomCatalogTest {
    private final NodeIdiomCatalog catalog = new NodeIdiomCatalog();

    @Test
    void mapsBasicConstructs() {
        assertEquals("assignment", catalog.idiomFor("MOVE"));
        assertEquals("expression", catalog.idiomFor("COMPUTE"));
        assertEquals("if", catalog.idiomFor("IF"));
        assertEquals("switch", catalog.idiomFor("EVALUATE"));
        assertEquals("function call", catalog.idiomFor("PERFORM"));
        assertEquals("await fs.readFile()", catalog.idiomFor("READ"));
        assertEquals("await fs.writeFile()", catalog.idiomFor("WRITE"));
        assertEquals("await prisma.model.findMany()", catalog.idiomFor("EXEC SQL"));
        assertEquals("console.log", catalog.idiomFor("DISPLAY"));
        assertEquals("req.body / req.params", catalog.idiomFor("ACCEPT"));
    }

    @Test
    void returnsManualForUnknown() {
        assertEquals("manual action item", catalog.idiomFor("UNKNOWN"));
    }

    @Test
    void allIdiomsContainsKnownKeys() {
        assertEquals(10, catalog.allIdioms().size());
    }
}
