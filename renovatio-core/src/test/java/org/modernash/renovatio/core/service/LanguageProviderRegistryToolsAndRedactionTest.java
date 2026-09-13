package org.modernash.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.shared.domain.Tool;
import org.modernash.renovatio.shared.domain.BasicTool;
import org.modernash.renovatio.shared.spi.LanguageProvider;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LanguageProviderRegistryToolsAndRedactionTest {

    static class ToolsProvider implements LanguageProvider {
        private final String name;
        ToolsProvider(String name) { this.name = name; }
        @Override public String language() { return name; }
        @Override public Set<Capabilities> capabilities() { return EnumSet.of(Capabilities.METRICS); }
        @Override public java.util.List<Tool> getTools() {
            BasicTool bt = new BasicTool("lang.metrics", "desc");
            bt.setInputSchema(java.util.Map.<String,Object>of());
            bt.setMetadata(java.util.Map.<String,Object>of());
            return List.of(bt);
        }
        @Override public org.modernash.renovatio.shared.domain.AnalyzeResult analyze(org.modernash.renovatio.shared.nql.NqlQuery query, org.modernash.renovatio.shared.domain.Workspace workspace) { return null; }
        @Override public org.modernash.renovatio.shared.domain.PlanResult plan(org.modernash.renovatio.shared.nql.NqlQuery query, org.modernash.renovatio.shared.domain.Scope scope, org.modernash.renovatio.shared.domain.Workspace workspace) { return null; }
        @Override public org.modernash.renovatio.shared.domain.ApplyResult apply(String planId, boolean dryRun, org.modernash.renovatio.shared.domain.Workspace workspace) { return null; }
        @Override public org.modernash.renovatio.shared.domain.DiffResult diff(String runId, org.modernash.renovatio.shared.domain.Workspace workspace) { return null; }
        @Override public Optional<org.modernash.renovatio.shared.domain.StubResult> generateStubs(org.modernash.renovatio.shared.nql.NqlQuery query, org.modernash.renovatio.shared.domain.Workspace workspace) { return Optional.empty(); }
        @Override public org.modernash.renovatio.shared.domain.MetricsResult metrics(org.modernash.renovatio.shared.domain.Scope scope, org.modernash.renovatio.shared.domain.Workspace workspace) { return new org.modernash.renovatio.shared.domain.MetricsResult(true, "ok"); }
    }

    @Test
    void registrationRejectsToolAndCapabilityConflict() {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        reg.registerProvider(new ToolsProvider("java"));
        assertThrows(IllegalStateException.class,
                () -> reg.registerProvider(new ToolsProvider("java")));
    }

    @Test
    void redactForLog_covers_depth_and_sensitive_keys() throws Exception {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        Method m = LanguageProviderRegistry.class.getDeclaredMethod("redactForLog", Object.class, int.class);
        m.setAccessible(true);
        Map<String, Object> nested = Map.of(
                "content", "x".repeat(200),
                "code", "print()",
                "a", Map.of("b", Map.of("c", Map.of("d", "e")))
        );
        Object out = m.invoke(reg, nested, 0);
        assertTrue(out.toString().contains("<redacted:"));
        Object red = m.invoke(reg, "y".repeat(200), 0);
        assertTrue(red.toString().length() < 200);
        Object deep = m.invoke(reg, Map.of("k", Map.of("k2", Map.of("k3", Map.of("k4", "v")))), 0);
        assertTrue(deep.toString().contains("<redacted>"));
    }
}
