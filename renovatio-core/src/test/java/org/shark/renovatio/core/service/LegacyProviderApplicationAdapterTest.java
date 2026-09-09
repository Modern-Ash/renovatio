package org.shark.renovatio.core.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyProviderApplicationAdapterTest {
    @Test void delegatesThroughOneApplicationBoundaryAndProtectsCallerArguments() {
        LanguageProviderRegistry registry = mock(LanguageProviderRegistry.class);
        when(registry.routeToolCall(eq("cobol.plan"), anyMap())).thenReturn(Map.of("success", true));
        var adapter = new LegacyProviderApplicationAdapter(registry);
        Map<String, Object> arguments = new LinkedHashMap<>(Map.of("projectId", "p"));
        assertEquals(Map.of("success", true), adapter.execute("cobol.plan", arguments));
        verify(registry).routeToolCall(eq("cobol.plan"), argThat(value -> value != arguments && value.equals(arguments)));
        assertThrows(IllegalArgumentException.class, () -> adapter.execute(" ", arguments));
    }
}
