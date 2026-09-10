package org.shark.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyProviderApplicationAdapterTest {
    @Test void delegatesThroughOneApplicationBoundaryAndProtectsCallerArguments() {
        LanguageProviderRegistry registry = mock(LanguageProviderRegistry.class);
        when(registry.routeToolCall(eq("cobol.plan"), anyMap()))
                .thenReturn(Map.of("success", true, "planId", "plan-1"));
        var adapter = new LegacyProviderApplicationAdapter(registry);
        Map<String, Object> arguments = new LinkedHashMap<>(Map.of("projectId", "p"));
        assertEquals("plan-1", adapter.execute("cobol.plan", arguments).get("planId"));
        verify(registry).routeToolCall(eq("cobol.plan"), argThat(value -> value != arguments && value.equals(arguments)));
        assertThrows(IllegalArgumentException.class, () -> adapter.execute(" ", arguments));
    }

    @Test void rejectsApplyWhenWorkspaceChangedAfterPlan(@TempDir Path workspace) throws Exception {
        LanguageProviderRegistry registry = mock(LanguageProviderRegistry.class);
        when(registry.routeToolCall(eq("cobol.plan"), anyMap()))
                .thenReturn(Map.of("success", true, "planId", "plan-1"));
        when(registry.routeToolCall(eq("cobol.apply"), anyMap()))
                .thenReturn(Map.of("success", true, "runId", "run-1"));
        var adapter = new LegacyProviderApplicationAdapter(registry);
        Path source = workspace.resolve("source.cob");
        Files.writeString(source, "IDENTIFICATION DIVISION.");
        Map<String, Object> plan = Map.of("workspacePath", workspace.toString());
        adapter.execute("cobol.plan", plan);

        Files.writeString(source, "IDENTIFICATION DIVISION.\nPROGRAM-ID. CHANGED.");
        Map<String, Object> apply = Map.of("workspacePath", workspace.toString(), "planId", "plan-1");
        Map<String, Object> result = adapter.execute("cobol.apply", apply);

        assertEquals(false, result.get("success"));
        assertTrue(result.get("message").toString().contains("stale plan rejected"));
        verify(registry, never()).routeToolCall(eq("cobol.apply"), anyMap());
    }
}
