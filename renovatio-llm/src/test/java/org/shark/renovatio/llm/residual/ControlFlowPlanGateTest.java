package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolValidator;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlFlowPlanGateTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HASH = "a".repeat(64);
    private static final String NODE = "b".repeat(64);
    private final ControlFlowPlanGate gate = new ControlFlowPlanGate(new ResidualAnnotationAssembler());

    @Test
    void greenCommitBoundCharacterizationRetainsAReviewOnlyPlan() {
        ControlFlowPlanGate.Decision decision = gate.retainIfEligible(empty(), plan(), context(),
                new CharacterizationEvidence("repo://evidence/characterization-green.json",
                        true, true, true));

        assertTrue(decision.proposalRetained());
        assertEquals("repo://evidence/characterization-green.json", decision.baselineRef());
        assertEquals(1, decision.sidecar().annotations().size());
        assertEquals(AnnotationReview.ReviewState.NEEDS_REVIEW,
                decision.sidecar().annotations().get(0).review().reviewState());
        assertTrue(new AnnotatedCobolValidator().validate(decision.sidecar(), HASH,
                Map.of(NODE, AnnotatedNodeKind.PARAGRAPH)).isEmpty());
    }

    @Test
    void redCharacterizationDiscardsProposalAndPreservesDeterministicSidecar() {
        AnnotatedCobolModel original = empty();
        ControlFlowPlanGate.Decision decision = gate.retainIfEligible(original, plan(), context(),
                new CharacterizationEvidence("repo://evidence/red.json", true, true, false));

        assertFalse(decision.proposalRetained());
        assertSame(original, decision.sidecar());
        assertTrue(decision.sidecar().annotations().isEmpty());
        assertEquals(ControlFlowPlanGate.DIAGNOSTIC, decision.diagnosticCode());
        assertEquals(ControlFlowPlanGate.MANUAL_ACTION, decision.manualAction());
    }

    @Test
    void missingEvidenceDiscardsProposalWithoutRunningTheAssembler() {
        AnnotatedCobolModel original = empty();
        ControlFlowPlanGate.Decision decision = gate.retainIfEligible(original, plan(), context(),
                CharacterizationEvidence.missing());

        assertFalse(decision.proposalRetained());
        assertSame(original, decision.sidecar());
        assertEquals(ControlFlowPlanGate.DIAGNOSTIC, decision.diagnosticCode());
    }

    @Test
    void compilationAndSchemaMustPrecedeCharacterization() {
        for (CharacterizationEvidence evidence : List.of(
                new CharacterizationEvidence("repo://baseline", false, true, true),
                new CharacterizationEvidence("repo://baseline", true, false, true))) {
            ControlFlowPlanGate.Decision decision = gate.retainIfEligible(empty(), plan(), context(), evidence);
            assertFalse(decision.proposalRetained());
            assertEquals(ControlFlowPlanGate.DIAGNOSTIC, decision.diagnosticCode());
        }
    }

    @Test
    void evidenceFromAnotherBaselineIsRejected() {
        ControlFlowPlanGate.Decision decision = gate.retainIfEligible(empty(), plan(), context(),
                new CharacterizationEvidence("repo://evidence/other-baseline.json", true, true, true));

        assertFalse(decision.proposalRetained());
        assertEquals(ControlFlowPlanGate.DIAGNOSTIC, decision.diagnosticCode());
        assertTrue(decision.sidecar().annotations().isEmpty());
    }

    private static AnnotatedCobolModel empty() {
        return new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION, "cobol-ir.v1", HASH, List.of());
    }

    private static ObjectNode plan() {
        ObjectNode plan = JSON.createObjectNode();
        plan.putArray("steps").add("Introduce an explicit state").add("Preserve both branch targets");
        plan.putArray("risks").add("Loop termination");
        return plan;
    }

    private static ResidualAnnotationContext context() {
        return new ResidualAnnotationContext("cobol-ir.v1", HASH, NODE, AnnotatedNodeKind.PARAGRAPH,
                "offline", "fake", "v1", "control-flow-plan.v1", HASH,
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS, 0.7,
                "project:owner", List.of(NODE), "repo://evidence/characterization-green.json",
                List.of(), false);
    }
}
