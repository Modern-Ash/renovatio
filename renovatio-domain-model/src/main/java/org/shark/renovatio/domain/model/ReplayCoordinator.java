package org.shark.renovatio.domain.model;

import java.util.LinkedHashMap;
import java.util.Set;

/** Runs both implementations against identical input and builds a comparison fixture. */
public final class ReplayCoordinator {
    private final ReplayRunner baseline;
    private final ReplayRunner candidate;
    public ReplayCoordinator(ReplayRunner baseline, ReplayRunner candidate) {
        this.baseline = java.util.Objects.requireNonNull(baseline);
        this.candidate = java.util.Objects.requireNonNull(candidate);
    }
    public EquivalenceFixture run(ReplayRunner.ReplayInput input, Set<String> ignoredFields) {
        var left = baseline.run(input); var right = candidate.run(input);
        return new EquivalenceFixture(input.caseId(), input.values(), envelope(left), envelope(right), ignoredFields);
    }
    private static java.util.Map<String, Object> envelope(ReplayRunner.ReplayResult result) {
        var map = new LinkedHashMap<String, Object>();
        map.put("status", result.status()); map.put("output", result.output());
        map.put("stateChanges", result.stateChanges()); map.put("externalCalls", result.externalCalls());
        map.put("error", result.error() == null ? "" : result.error()); return map;
    }
}
