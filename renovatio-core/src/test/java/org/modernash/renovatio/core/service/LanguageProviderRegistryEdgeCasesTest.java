package org.modernash.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.shared.spi.LanguageProvider;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LanguageProviderRegistryEdgeCasesTest {

    static class SimpleProvider implements LanguageProvider {
        @Override public String language() { return "java"; }
        @Override public Set<Capabilities> capabilities() { return EnumSet.of(Capabilities.ANALYZE, Capabilities.PLAN, Capabilities.APPLY, Capabilities.DIFF, Capabilities.METRICS); }
        @Override public org.modernash.renovatio.shared.domain.AnalyzeResult analyze(org.modernash.renovatio.shared.nql.NqlQuery query, org.modernash.renovatio.shared.domain.Workspace workspace) { return new org.modernash.renovatio.shared.domain.AnalyzeResult(true, "ok"); }
        @Override public org.modernash.renovatio.shared.domain.PlanResult plan(org.modernash.renovatio.shared.nql.NqlQuery query, org.modernash.renovatio.shared.domain.Scope scope, org.modernash.renovatio.shared.domain.Workspace workspace) { return new org.modernash.renovatio.shared.domain.PlanResult(true, "ok"); }
        @Override public org.modernash.renovatio.shared.domain.ApplyResult apply(String planId, boolean dryRun, org.modernash.renovatio.shared.domain.Workspace workspace) { return new org.modernash.renovatio.shared.domain.ApplyResult(true, "ok"); }
        @Override public org.modernash.renovatio.shared.domain.DiffResult diff(String runId, org.modernash.renovatio.shared.domain.Workspace workspace) { return new org.modernash.renovatio.shared.domain.DiffResult(true, "ok"); }
        @Override public Optional<org.modernash.renovatio.shared.domain.StubResult> generateStubs(org.modernash.renovatio.shared.nql.NqlQuery query, org.modernash.renovatio.shared.domain.Workspace workspace) { return Optional.empty(); }
        @Override public org.modernash.renovatio.shared.domain.MetricsResult metrics(org.modernash.renovatio.shared.domain.Scope scope, org.modernash.renovatio.shared.domain.Workspace workspace) { return new org.modernash.renovatio.shared.domain.MetricsResult(true, "ok"); }
        @Override public java.util.List<org.modernash.renovatio.shared.domain.Tool> getTools() { return java.util.List.of(); }
    }

    @Test
    void invalid_tool_name_returns_error() {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        Map<String, Object> r = reg.routeToolCall("invalid", Map.of("workspacePath", "."));
        assertEquals(false, r.get("success"));
        assertEquals("error", r.get("type"));
    }

    @Test
    void no_provider_for_language_returns_error() {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        Map<String, Object> r = reg.routeToolCall("java.analyze", Map.of("workspacePath", "."));
        assertEquals(false, r.get("success"));
        assertEquals("error", r.get("type"));
    }

    @Test
    void unknown_capability_default_branch_and_underscore_recipe() {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        reg.registerProvider(new SimpleProvider());

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("workspacePath", ".");

        Map<String, Object> m = reg.routeToolCall("java.unknowncap", args);
        assertEquals(false, m.get("success"));
        assertEquals("error", m.get("type"));

        Map<String, Object> m2 = reg.routeToolCall("java.plan_myRecipe", new LinkedHashMap<>(args));
        assertEquals(true, m2.get("success"));
        assertEquals("plan", m2.get("type"));
    }
}

