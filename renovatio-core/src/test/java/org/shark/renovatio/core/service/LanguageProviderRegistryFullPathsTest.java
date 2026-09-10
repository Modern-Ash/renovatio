package org.shark.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.LanguageProvider;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LanguageProviderRegistryFullPathsTest {

    static class CapturingProvider implements LanguageProvider {
        Scope lastScope;
        Workspace lastWorkspace;
        NqlQuery lastQuery;

        @Override public String language() { return "java"; }
        @Override public Set<Capabilities> capabilities() {
            return EnumSet.allOf(Capabilities.class);
        }
        @Override public AnalyzeResult analyze(NqlQuery query, Workspace workspace) {
            this.lastQuery = query; this.lastWorkspace = workspace;
            AnalyzeResult r = new AnalyzeResult(true, "an-ok");
            r.setRunId("rid");
            r.setData(new LinkedHashMap<>(Map.of(
                    "summary", "s",
                    "issues", List.of("i1"),
                    "metrics", Map.of("m", 1),
                    "diffs", List.of("d1"),
                    "analyzedFiles", List.of("f1"),
                    "applied", true
            )));
            r.setAst(Map.of("ast","x"));
            r.setSymbols(Map.of("sym",1));
            r.setDependencies(Map.of("dep","y"));
            r.setPerformance(new PerformanceMetrics(5));
            return r;
        }
        @Override public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) {
            this.lastScope = scope; this.lastWorkspace = workspace; this.lastQuery = query;
            PlanResult r = new PlanResult(true, "pl-ok");
            r.setPlanId("pid");
            r.setPlanContent("content");
            r.setSteps(Map.of("s", 2));
            return r;
        }
        @Override public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) {
            this.lastWorkspace = workspace;
            ApplyResult r = new ApplyResult(true, "ap-ok");
            r.setDryRun(dryRun);
            r.setDiff("diff");
            r.setChanges(Map.of("k", "v"));
            return r;
        }
        @Override public DiffResult diff(String runId, Workspace workspace) {
            this.lastWorkspace = workspace;
            DiffResult r = new DiffResult(true, "df-ok");
            r.setUnifiedDiff("u");
            r.setSemanticDiff(Map.of("s",1));
            r.setHunks(Map.of());
            return r;
        }
        @Override public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) { return Optional.empty(); }
        @Override public MetricsResult metrics(Scope scope, Workspace workspace) {
            this.lastScope = scope; this.lastWorkspace = workspace;
            MetricsResult r = new MetricsResult(true, "mt-ok");
            r.setMetrics(Map.of("a", 1));
            r.setDetails(Map.of("k", "v"));
            return r;
        }
        @Override public List<Tool> getTools() { return List.of(); }
    }

    @Test
    void covers_all_capabilities_and_scope_defaults() {
        LanguageProviderRegistry registry = new LanguageProviderRegistry();
        CapturingProvider p = new CapturingProvider();
        registry.registerProvider(p);

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("workspacePath", ".");

        Map<String, Object> m1 = registry.routeToolCall("java.analyze", new LinkedHashMap<>(args));
        assertEquals(true, m1.get("success"));
        assertEquals("analyze", m1.get("type"));
        assertEquals("default", p.lastWorkspace.getId());
        assertEquals(".", p.lastWorkspace.getPath());

        Map<String, Object> projectArgs = new LinkedHashMap<>(args);
        projectArgs.put("projectId", "/projects/bank");
        registry.routeToolCall("java.analyze", projectArgs);
        assertEquals("/projects/bank", p.lastWorkspace.getId());
        assertFalse(p.lastQuery.getParameters().containsKey("projectId"));

        Map<String, Object> m2 = registry.routeToolCall("java.metrics", new LinkedHashMap<>(args));
        assertEquals(true, m2.get("success"));
        assertEquals("metrics", m2.get("type"));
        assertEquals(List.of("**/*"), p.lastScope.getIncludePatterns());

        Map<String, Object> m3 = registry.routeToolCall("java.plan", new LinkedHashMap<>(args));
        assertEquals(true, m3.get("success"));
        assertEquals("plan", m3.get("type"));

        // Pass the same args map to capture the injected recipeId
        Map<String, Object> m4 = registry.routeToolCall("java.apply.myRecipe", args);
        assertEquals(true, m4.get("success"));
        assertEquals("apply", m4.get("type"));
        assertEquals(true, m4.get("dryRun"));
        // recipeId injected
        assertEquals("myRecipe", args.putIfAbsent("recipeId", "x"));

        Map<String, Object> m5 = registry.routeToolCall("java.diff", new LinkedHashMap<>(args));
        assertEquals(true, m5.get("success"));
        assertEquals("diff", m5.get("type"));

        // Provide scope as list and string to cover createScope branches
        Map<String, Object> argsList = new LinkedHashMap<>(args);
        argsList.put("scope", java.util.Arrays.asList("src/**", "  ", null, "test/**"));
        registry.routeToolCall("java.metrics", argsList);
        assertEquals(List.of("src/**", "test/**"), p.lastScope.getIncludePatterns());

        Map<String, Object> argsStr = new LinkedHashMap<>(args);
        argsStr.put("scope", "src/main/**");
        registry.routeToolCall("java.metrics", argsStr);
        assertEquals(List.of("src/main/**"), p.lastScope.getIncludePatterns());
    }
}
