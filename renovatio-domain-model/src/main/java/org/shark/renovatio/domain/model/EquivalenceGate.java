package org.shark.renovatio.domain.model;

/** Policy gate: reports readiness; never performs cutover itself. */
public final class EquivalenceGate {
    private EquivalenceGate() { }
    public static Decision evaluate(int totalCases, int equivalentCases, double minimumRate) {
        if (totalCases < 0 || equivalentCases < 0 || equivalentCases > totalCases || minimumRate < 0 || minimumRate > 1)
            throw new IllegalArgumentException("invalid equivalence metrics");
        double rate = totalCases == 0 ? 0 : (double) equivalentCases / totalCases;
        return new Decision(rate >= minimumRate && totalCases > 0, rate, totalCases, equivalentCases);
    }
    public record Decision(boolean readyForHumanCutoverReview, double equivalenceRate, int totalCases, int equivalentCases) { }
}
