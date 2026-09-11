package org.shark.renovatio.mcp.server.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.application.spi.ApplicationCommandBus;
import org.shark.renovatio.core.service.LanguageProviderRegistry;
import org.shark.renovatio.mcp.server.model.McpTool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpToolingServiceCapabilityTest {
    @Test
    void listsAndExecutesCapabilitiesToolFromSharedContract() {
        LanguageProviderRegistry providers = mock(LanguageProviderRegistry.class);
        when(providers.generateTools()).thenReturn(List.of());
        McpToolingService service = new McpToolingService(
                providers,
                mock(ApplicationCommandBus.class),
                mock(McpToolAdapter.class),
                "2024-11-05");

        List<McpTool> tools = service.getMcpTools();
        assertEquals("renovatio_capabilities", tools.get(0).getName());
        assertEquals("2026-09-11.ac09", tools.get(0).getMetadata().get("capabilityContractVersion"));
        assertEquals("renovatio_capabilities", service.getTool("renovatio_capabilities").getName());
        assertEquals("renovatio_capabilities", service.getTool("renovatio.capabilities").getName());

        Map<String, Object> result = service.executeTool("renovatio_capabilities", Map.of());
        assertEquals(true, result.get("success"));
        assertEquals("renovatio.surface-capabilities", result.get("id"));
        assertFalse(((List<?>) result.get("capabilities")).isEmpty());
        assertTrue(((List<?>) result.get("surfaces")).contains("mcp"));
        assertTrue(((List<?>) result.get("capabilities")).stream()
                .map(Map.class::cast)
                .anyMatch(capability -> "reference-pipeline".equals(capability.get("id"))
                        && "project-member".equals(capability.get("authorization"))
                        && ((List<?>) capability.get("errors")).contains("CAPABILITY_UNAVAILABLE")
                        && "supported".equals(((Map<?, ?>) capability.get("runtime")).get("equivalence"))));
    }

    @Test
    void includesCapabilitiesToolWhenLanguageFilterIsApplied() {
        LanguageProviderRegistry providers = mock(LanguageProviderRegistry.class);
        when(providers.generateTools()).thenReturn(List.of());
        McpToolingService service = new McpToolingService(
                providers,
                mock(ApplicationCommandBus.class),
                mock(McpToolAdapter.class),
                "2024-11-05");

        assertEquals("renovatio_capabilities", service.getMcpTools("java").get(0).getName());
    }

    @Test
    void rejectsMcpWorkspaceFileAccessOutsideProcessRoot() throws Exception {
        LanguageProviderRegistry providers = mock(LanguageProviderRegistry.class);
        when(providers.generateTools()).thenReturn(List.of());
        McpToolingService service = new McpToolingService(
                providers,
                mock(ApplicationCommandBus.class),
                mock(McpToolAdapter.class),
                "2024-11-05");

        assertThrows(SecurityException.class, () -> service.readFileContent("/etc/passwd"));
        assertThrows(SecurityException.class, () -> service.writeFileContent("../escape.txt", "nope"));

        Path local = Path.of("target", "mcp-security-test.txt");
        Files.createDirectories(local.getParent());
        service.writeFileContent(local.toString(), "ok");
        assertEquals("ok", service.readFileContent(local.toString()));
    }
}
