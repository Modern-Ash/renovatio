package org.shark.renovatio.domain.model;
import java.util.List; import java.util.Set;
/** Executes a suite of replay inputs and summarizes equivalence for CI gates. */
public final class ReplayHarness {
 private final ReplayCoordinator coordinator;
 public ReplayHarness(ReplayCoordinator coordinator){this.coordinator=java.util.Objects.requireNonNull(coordinator);}
 public HarnessResult run(List<ReplayRunner.ReplayInput> inputs, Set<String> ignoredFields, double minimumRate){
  var fixtures=(inputs==null?List.<ReplayRunner.ReplayInput>of():inputs).stream().map(i->coordinator.run(i,ignoredFields)).toList();
  int equivalent=(int)fixtures.stream().filter(f->f.compare().equivalent()).count();
  var gate=EquivalenceGate.evaluate(fixtures.size(),equivalent,minimumRate);
  return new HarnessResult(fixtures,gate);
 }
 public record HarnessResult(List<EquivalenceFixture> fixtures, EquivalenceGate.Decision gate){ }
}
