package org.shark.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.LanguageProvider;
import org.shark.renovatio.shared.spi.ExtendedLanguageProvider;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LanguageProviderRegistryErrorPathsTest {

    @Test
    void returnsErrorForInvalidToolName() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();
        Map<String, Object> res = registry.routeToolCall("invalid", Map.of());
        assertEquals(false, res.get("success"));
        assertTrue(String.valueOf(res.get("message")).toLowerCase().contains("invalid tool name"));
        assertEquals("error", res.get("type"));
    }

    @Test
    void returnsErrorWhenNoProviderFound() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();
        Map<String, Object> res = registry.routeToolCall("kotlin.metrics", Map.of());
        assertEquals(false, res.get("success"));
        assertTrue(String.valueOf(res.get("message")).contains("No provider"));
        assertEquals("error", res.get("type"));
    }

    @Test
    void returnsErrorWhenCapabilityUnsupported() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();
        LanguageProvider provider = mock(LanguageProvider.class);
        when(provider.language()).thenReturn("java");
        when(provider.capabilities()).thenReturn(Set.of());
        registry.registerProvider(provider);

        Map<String, Object> res = registry.routeToolCall("java.unknownCap", Map.of());
        assertEquals(false, res.get("success"));
        assertTrue(String.valueOf(res.get("message")).toLowerCase().contains("unsupported capability"));
        assertEquals("error", res.get("type"));
    }

    @Test
    void routesToExtendedProviderWhenCapabilityUnknown() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();
        class ExtendedStub implements ExtendedLanguageProvider, LanguageProvider {
            @Override public String language() { return "java"; }
            @Override public Set<Capabilities> capabilities() { return Set.of(); }
            @Override public AnalyzeResult analyze(NqlQuery query, Workspace workspace) { return new AnalyzeResult(true, "ok"); }
            @Override public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) { return new PlanResult(true, "ok"); }
            @Override public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) { return new ApplyResult(true, "ok"); }
            @Override public DiffResult diff(String runId, Workspace workspace) { return new DiffResult(true, "ok"); }
            @Override public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) { return Optional.empty(); }
            @Override public MetricsResult metrics(Scope scope, Workspace workspace) { return new MetricsResult(true, "ok"); }
            @Override public List<Tool> getTools() { return List.of(); }
            @Override public Map<String, Object> executeExtendedTool(String capabilityKey, Map<String, Object> args) {
                return Map.of("success", true, "type", "extended", "capability", capabilityKey);
            }
        }
        LanguageProvider ext = new ExtendedStub();
        registry.registerProvider(ext);

        Map<String, Object> res = registry.routeToolCall("java.custom_capability", Map.of("workspacePath", "."));
        assertEquals(true, res.get("success"));
        assertEquals("extended", res.get("type"));
        assertEquals("custom_capability", res.get("capability"));
    }

    @Test
    void preservesStructuredUnavailableTargetForMcpAdapters() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();
        class UnavailableStub implements ExtendedLanguageProvider, LanguageProvider {
            @Override public String language() { return "cobol"; }
            @Override public Set<Capabilities> capabilities() { return Set.of(); }
            @Override public AnalyzeResult analyze(NqlQuery query, Workspace workspace) { return null; }
            @Override public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) { return null; }
            @Override public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) { return null; }
            @Override public DiffResult diff(String runId, Workspace workspace) { return null; }
            @Override public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) { return Optional.empty(); }
            @Override public MetricsResult metrics(Scope scope, Workspace workspace) { return null; }
            @Override public List<Tool> getTools() { return List.of(); }
            @Override public Map<String, Object> executeExtendedTool(String capability, Map<String, Object> args) {
                new TargetEmitterRegistry(List.of()).resolve(MigrationProfile.Language.NODE);
                return Map.of();
            }
        }
        registry.registerProvider(new UnavailableStub());

        Map<String, Object> result = registry.routeToolCall("cobol.migrate_db2", Map.of());

        assertEquals(false, result.get("success"));
        assertEquals("TARGET_EMITTER_UNAVAILABLE", result.get("code"));
        assertEquals("NODE", result.get("requestedTarget"));
        assertEquals(List.of(), result.get("availableTargets"));
    }
}
