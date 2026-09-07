package org.shark.renovatio.shared.equivalence;

/** Executes two fixture runners and emits release-gateable deterministic evidence. */
public final class EquivalenceHarness {
    private final EquivalenceComparator comparator = new EquivalenceComparator();
    public EquivalenceReport evaluate(EquivalenceRunner.Fixture fixture, EquivalenceRunner cobol, EquivalenceRunner target, boolean intentionalChange) throws Exception {
        var source = cobol.run(fixture); var candidate = target.run(fixture);
        var result = comparator.compare(source.outputHash(), candidate.outputHash(), source.invariants(), candidate.invariants(), intentionalChange);
        return new EquivalenceReport(fixture.id(), source.outputHash(), candidate.outputHash(), source.invariants(), candidate.invariants(), result.classification(), result.reason());
    }
}
