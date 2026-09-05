package org.shark.renovatio.domain.model;
import org.junit.jupiter.api.Test; import java.util.List; import java.util.Map; import static org.junit.jupiter.api.Assertions.assertTrue;
class ReplayHarnessTest { @Test void runsSuiteAndEvaluatesGate(){ ReplayRunner r=i->new ReplayRunner.ReplayResult("OK",Map.of("case",i.caseId()),null,null,null); var out=new ReplayHarness(new ReplayCoordinator(r,r)).run(List.of(new ReplayRunner.ReplayInput("a",null)),null,1); assertTrue(out.gate().readyForHumanCutoverReview()); } }
