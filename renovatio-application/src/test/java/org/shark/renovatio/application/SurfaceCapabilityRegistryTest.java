package org.shark.renovatio.application;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.application.capability.SurfaceCapabilityRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceCapabilityRegistryTest {
    @Test
    void exposesVersionedContractForEverySurface() {
        Map<String, Object> document = new SurfaceCapabilityRegistry().asMap();

        assertEquals(SurfaceCapabilityRegistry.DOCUMENT_ID, document.get("id"));
        assertEquals(SurfaceCapabilityRegistry.CONTRACT_VERSION, document.get("version"));
        assertTrue(((List<?>) document.get("surfaces")).containsAll(
                List.of("api", "cli", "mcp", "workbench")));
        assertTrue(((List<?>) document.get("capabilities")).stream()
                .map(Map.class::cast)
                .anyMatch(capability -> "cobol.analyze".equals(capability.get("id"))
                        && "stable".equals(capability.get("maturity"))
                        && "supported".equals(((Map<?, ?>) capability.get("surfaces")).get("api"))
                        && "supported".equals(((Map<?, ?>) capability.get("surfaces")).get("mcp"))));
        assertTrue(((List<?>) document.get("capabilities")).stream()
                .map(Map.class::cast)
                .anyMatch(capability -> "python.target".equals(capability.get("id"))
                        && "planned".equals(capability.get("maturity"))));
    }

    @Test
    void declaresCommonSemanticsForSupportedCapabilities() {
        Map<String, Object> capability = capability("reference-pipeline");

        assertEquals("project-member", capability.get("authorization"));
        assertTrue(((List<?>) capability.get("states")).containsAll(
                List.of("accepted", "planned", "running", "succeeded", "failed", "unavailable")));
        assertTrue(((List<?>) capability.get("errors")).containsAll(
                List.of("VALIDATION_FAILED", "CAPABILITY_UNAVAILABLE", "AUTHORIZATION_DENIED", "STALE_INPUT")));
        assertTrue(((List<?>) capability.get("manifestHashes")).containsAll(
                List.of("sourceTreeHash", "profileHash", "manifestHash", "equivalenceHash")));

        Map<?, ?> runtime = (Map<?, ?>) capability.get("runtime");
        assertEquals("supported", runtime.get("persistence"));
        assertEquals("supported", runtime.get("equivalence"));
        assertEquals("planned", runtime.get("llm"));
    }

    private static Map<String, Object> capability(String id) {
        Optional<Map<String, Object>> capability = ((List<?>) new SurfaceCapabilityRegistry().asMap().get("capabilities"))
                .stream()
                .map(entry -> (Map<String, Object>) entry)
                .filter(entry -> id.equals(entry.get("id")))
                .findFirst();
        assertTrue(capability.isPresent(), "Expected capability " + id);
        return capability.orElseThrow();
    }
}
