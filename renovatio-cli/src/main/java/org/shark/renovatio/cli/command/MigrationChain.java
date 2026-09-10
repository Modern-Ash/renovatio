package org.shark.renovatio.cli.command;

import org.shark.renovatio.cli.WorkspaceStateStore.PlanDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Rebuilds the in-memory plan/run state that {@code MigrationPlanService} keeps per process, by
 * replaying {@code cobol.plan} (and optionally {@code cobol.apply}) from a stored
 * {@link PlanDescriptor}. Plans and runs are pure functions of {@code (nql, scope, workspace)}, so
 * a replay in a fresh process reproduces an equivalent internal state.
 */
final class MigrationChain {

    private final BiFunction<String, Map<String, Object>, Map<String, Object>> router;

    MigrationChain(BiFunction<String, Map<String, Object>, Map<String, Object>> router) {
        this.router = router;
    }

    /** Result of a replay step: the engine result plus the engine-side id it produced. */
    record Step(Map<String, Object> result, String engineId) {
        boolean ok() {
            return Boolean.TRUE.equals(result.get("success"));
        }
    }

    Step replayPlan(PlanDescriptor descriptor) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("workspacePath", descriptor.workspacePath());
        args.put("projectId", descriptor.workspacePath());
        put(args, "scope", descriptor.scope());
        put(args, "nql", descriptor.nql());
        put(args, "strategy", descriptor.strategy());
        put(args, "framework", descriptor.framework());
        Map<String, Object> result = router.apply("cobol.plan", args);
        return new Step(result, asString(result.get("planId")));
    }

    Step apply(String enginePlanId, String workspacePath, String projectId, boolean dryRun, String outputDir) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("workspacePath", workspacePath);
        args.put("projectId", projectId);
        args.put("planId", enginePlanId);
        args.put("dryRun", Boolean.toString(dryRun));
        put(args, "outputDir", outputDir);
        Map<String, Object> result = router.apply("cobol.apply", args);
        return new Step(result, asString(result.get("runId")));
    }

    Map<String, Object> diff(String engineRunId, String workspacePath) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("workspacePath", workspacePath);
        args.put("runId", engineRunId);
        return router.apply("cobol.diff", args);
    }

    private static void put(Map<String, Object> args, String key, String value) {
        if (value != null && !value.isBlank()) {
            args.put(key, value);
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
