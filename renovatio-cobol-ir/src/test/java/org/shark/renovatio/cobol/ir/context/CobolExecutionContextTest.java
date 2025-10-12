package org.shark.renovatio.cobol.ir.context;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CobolExecutionContextTest {

    @Test
    void builder_shouldRegisterVariables_andAttributes_andResolveScope() {
        CobolExecutionContext ctx = CobolExecutionContext.builder()
                .registerVariable("ws-num", "WORKING-STORAGE")
                .registerVariables(Set.of("LNK-ITEM"), "LINKAGE")
                .attribute("programId", "DEMO")
                .attribute(null, "X")
                .build();

        assertEquals("working-storage", ctx.resolveScope("WS-NUM").orElseThrow());
        assertEquals("linkage", ctx.resolveScope("lnk-item").orElseThrow());
        assertTrue(ctx.resolveScope(null).isEmpty());
        assertEquals("DEMO", ctx.attribute("programId").orElseThrow());
        assertTrue(ctx.attribute("missing").isEmpty());
    }
}
