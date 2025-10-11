package org.shark.renovatio.shared.spi;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BaseLanguageProviderTest {

    static class DummyProvider extends BaseLanguageProvider {
        @Override public String language() { return "dummy"; }
        @Override public Set<LanguageProvider.Capabilities> capabilities() { return EnumSet.noneOf(LanguageProvider.Capabilities.class); }
        @Override public AnalyzeResult analyze(NqlQuery query, Workspace workspace) { return new AnalyzeResult(true, "ok"); }
        @Override public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) { return new PlanResult(true, "ok"); }
        @Override public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) { return new ApplyResult(true, "ok"); }
        @Override public DiffResult diff(String runId, Workspace workspace) { return new DiffResult(true, "ok"); }
        @Override public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) { return Optional.empty(); }
        @Override public MetricsResult metrics(Scope scope, Workspace workspace) { return new MetricsResult(true, "ok"); }
        @Override public List<Tool> getTools() { return List.of(); }
        String exposeRunId() { return generateRunId(); }
        String exposePlanId() { return generatePlanId(); }
        String exposeDiff() { return createSampleDiff(); }
    }

    @Test
    void protected_helpers_areCovered() {
        DummyProvider p = new DummyProvider();
        String rid = p.exposeRunId();
        String pid = p.exposePlanId();
        String diff = p.exposeDiff();
        assertTrue(rid.startsWith("dummy-run-"));
        assertTrue(pid.startsWith("dummy-plan-"));
        assertTrue(diff.contains("--- a/"));
        assertTrue(diff.contains("+++ b/"));
    }
}

