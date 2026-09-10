package org.shark.renovatio.shared.equivalence;

import java.util.List;

/** Deterministic local release gate; operational approval remains outside this class. */
public final class EquivalenceReleaseGate {
    public Decision evaluate(List<EquivalenceReport> reports) {
        List<String> blockers = (reports == null ? List.<EquivalenceReport>of() : reports).stream()
                .filter(EquivalenceReport::blocksRelease).map(EquivalenceReport::fixtureId).sorted().toList();
        return new Decision(!blockers.isEmpty() ? false : reports != null && !reports.isEmpty(), blockers);
    }
    public record Decision(boolean ready, List<String> blockingFixtures) { }
}
