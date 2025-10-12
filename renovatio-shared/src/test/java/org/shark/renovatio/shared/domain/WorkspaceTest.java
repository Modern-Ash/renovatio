package org.shark.renovatio.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceTest {

    @Test
    void getters_setters_and_constructors() {
        Workspace w = new Workspace();
        w.setId("id");
        w.setPath("/tmp");
        w.setBranch("main");
        w.setMetadata(Map.of("k","v"));
        assertEquals("id", w.getId());
        assertEquals("/tmp", w.getPath());
        assertEquals("main", w.getBranch());
        assertEquals("v", w.getMetadata().get("k"));

        Workspace w2 = new Workspace("id2", "/repo", "feat");
        assertEquals("id2", w2.getId());
        assertEquals("/repo", w2.getPath());
        assertEquals("feat", w2.getBranch());
    }
}

