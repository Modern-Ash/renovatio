package org.shark.renovatio.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProviderResultsTest {

    @Test
    void analyzeResult_getters_setters_and_constructors() {
        AnalyzeResult r = new AnalyzeResult();
        r.setAst(Map.of("k","v"));
        r.setDependencies(Map.of());
        r.setSymbols(Map.of("s", 1));
        r.setData(Map.of());
        PerformanceMetrics pm = new PerformanceMetrics(123);
        r.setPerformance(pm);
        assertEquals("v", r.getAst().get("k"));
        assertEquals(1, r.getSymbols().get("s"));
        assertEquals(123, r.getPerformance().getExecutionTimeMs());

        AnalyzeResult r2 = new AnalyzeResult(true, "ok");
        assertTrue(r2.isSuccess());
        assertEquals("ok", r2.getMessage());
        assertTrue(r2.getTimestamp() > 0);
        r2.setRunId("rid");
        r2.setMetadata(Map.of("m", "x"));
        assertEquals("rid", r2.getRunId());
        assertEquals("x", r2.getMetadata().get("m"));
    }

    @Test
    void applyResult_getters_setters_and_constructors() {
        ApplyResult r = new ApplyResult(false, "msg");
        r.setDiff("diff");
        r.setChanges(Map.of("c", 1));
        r.setDryRun(true);
        r.setModifiedFiles(List.of("a"));
        assertFalse(r.isSuccess());
        assertEquals("msg", r.getMessage());
        assertEquals("diff", r.getDiff());
        assertEquals(1, r.getChanges().get("c"));
        assertTrue(r.isDryRun());
        assertEquals(List.of("a"), r.getModifiedFiles());
    }

    @Test
    void diff_and_plan_and_stub_results() {
        DiffResult d = new DiffResult(true, "ok");
        d.setUnifiedDiff("u");
        d.setSemanticDiff(Map.of("k", "v"));
        d.setHunks(Map.of());
        assertEquals("u", d.getUnifiedDiff());
        assertEquals("v", d.getSemanticDiff().get("k"));

        PlanResult p = new PlanResult();
        p.setPlanId("pid");
        p.setPlanContent("content");
        p.setSteps(Map.of("s", 2));
        assertEquals("pid", p.getPlanId());
        assertEquals("content", p.getPlanContent());
        assertEquals(2, p.getSteps().get("s"));

        StubResult s = new StubResult(false, "e");
        s.setTargetLanguage("java");
        s.setGeneratedFiles(Map.of("F","G"));
        s.setStubTemplate("tmpl");
        s.setGeneratedCode(Map.of("A","B"));
        assertEquals("java", s.getTargetLanguage());
        assertEquals("G", s.getGeneratedFiles().get("F"));
        assertEquals("tmpl", s.getStubTemplate());
        assertEquals("B", s.getGeneratedCode().get("A"));
    }

    @Test
    void performanceMetrics_getters_setters_and_constructor() {
        PerformanceMetrics pm = new PerformanceMetrics();
        pm.setExecutionTimeMs(99);
        assertEquals(99, pm.getExecutionTimeMs());
        PerformanceMetrics pm2 = new PerformanceMetrics(100);
        assertEquals(100, pm2.getExecutionTimeMs());
    }
}

