package org.shark.renovatio.shared.equivalence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EquivalenceComparatorTest {
    private final EquivalenceComparator comparator = new EquivalenceComparator();
    @Test void classifiesHashesAndInvariantDeltas() {
        assertEquals(EquivalenceClassification.EQUIVALENT, comparator.compare("a", "a", Map.of("balance", "10"), Map.of("balance", "10"), false).classification());
        assertEquals(EquivalenceClassification.REGRESSION, comparator.compare("a", "b", Map.of(), Map.of(), false).classification());
        assertEquals(EquivalenceClassification.INTENTIONAL_CHANGE, comparator.compare("a", "b", Map.of(), Map.of(), true).classification());
        assertEquals(EquivalenceClassification.UNDETERMINED, comparator.compare("", "b", Map.of(), Map.of(), false).classification());
    }
    @Test void reportBlocksOnlyUnsafeOutcomes() {
        assertEquals(true, new EquivalenceReport("fixture", "a", "b", Map.of(), Map.of(), EquivalenceClassification.REGRESSION, "delta").blocksRelease());
        assertEquals(false, new EquivalenceReport("fixture", "a", "a", Map.of(), Map.of(), EquivalenceClassification.EQUIVALENT, "match").blocksRelease());
    }
    @Test void numericComparisonIgnoresCobolPresentationZeros() {
        assertEquals(true, NumericEquivalence.equivalent("0950", "950.00"));
        assertEquals(false, NumericEquivalence.equivalent("0950", "951"));
    }
    @Test void commandRunnerCapturesExitAndOutput() throws Exception {
        var observation = new CommandObservationRunner(List.of("sh", "-c", "printf 0950")).run(new EquivalenceRunner.Fixture("fixture", "input"));
        assertEquals("0", observation.invariants().get("exitCode"));
        assertEquals("0950", observation.invariants().get("output"));
    }
    @Test void harnessUsesRunnerObservations() throws Exception {
        var fixture = new EquivalenceRunner.Fixture("payroll", "input");
        var runner = (EquivalenceRunner) value -> new EquivalenceRunner.Observation("output", Map.of("balance", "10"));
        assertEquals(EquivalenceClassification.EQUIVALENT, new EquivalenceHarness().evaluate(fixture, runner, runner, false).classification());
    }
    @Test void reportPreservesBothSidesOfAnInvariantDelta() throws Exception {
        var fixture = new EquivalenceRunner.Fixture("payroll", "input");
        var source = (EquivalenceRunner) value -> new EquivalenceRunner.Observation("same", Map.of("balance", "950"));
        var target = (EquivalenceRunner) value -> new EquivalenceRunner.Observation("same", Map.of("balance", "951"));
        var report = new EquivalenceHarness().evaluate(fixture, source, target, false);
        assertEquals(EquivalenceClassification.REGRESSION, report.classification());
        assertEquals("950", report.sourceInvariants().get("balance"));
        assertEquals("951", report.targetInvariants().get("balance"));
        assertEquals(true, report.blocksRelease());
    }
    @Test void evidenceWriterIsStableAndIncludesTheReleaseGate() throws Exception {
        var report = new EquivalenceReport("fixture", "source", "target", Map.of("z", "1", "a", "2"),
                Map.of("a", "3"), EquivalenceClassification.REGRESSION, "invariant-delta");
        String rendered = EquivalenceEvidenceWriter.render(report);
        Path destination = Files.createTempFile("equivalence-evidence", ".json");
        EquivalenceEvidenceWriter.write(destination, report);
        assertEquals(rendered, Files.readString(destination));
        assertEquals(true, rendered.contains("\"blocksRelease\": true"));
        assertEquals(true, rendered.indexOf("\"a\": \"2\"") < rendered.indexOf("\"z\": \"1\""));
    }
    @Test void recordAndBusinessRuleComparatorsExposeOnlyMeaningfulDeltas() {
        assertEquals(true, RecordEquivalence.equivalent(Map.of("account", "A", "balance", "950"), Map.of("balance", "950", "account", "A")));
        assertEquals("950 -> 951", RecordEquivalence.differences(Map.of("balance", "950"), Map.of("balance", "951")).get("balance"));
        assertEquals(Map.of("balance", "950 -> 951"), BusinessRuleEquivalence.violations(Map.of("balance", "950", "trace", "a"), Map.of("balance", "951", "trace", "b"), java.util.Set.of("balance")));
    }
    @Test void releaseGateRejectsAnyUnsafeFixtureAndEmptyEvidence() {
        var equivalent = new EquivalenceReport("ok", "a", "a", Map.of(), Map.of(), EquivalenceClassification.EQUIVALENT, "hash-match");
        var regression = new EquivalenceReport("bad", "a", "b", Map.of(), Map.of(), EquivalenceClassification.REGRESSION, "hash-delta");
        assertEquals(true, new EquivalenceReleaseGate().evaluate(List.of(equivalent)).ready());
        assertEquals(List.of("bad"), new EquivalenceReleaseGate().evaluate(List.of(equivalent, regression)).blockingFixtures());
        assertEquals(false, new EquivalenceReleaseGate().evaluate(List.of()).ready());
    }
}
