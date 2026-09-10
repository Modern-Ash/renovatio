package org.shark.renovatio.llm.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.F1DecisionCatalog;
import org.shark.renovatio.llm.residual.CharacterizationEvidence;
import org.shark.renovatio.llm.residual.ControlFlowPlanGate;
import org.shark.renovatio.llm.residual.ResidualAnnotationAssembler;
import org.shark.renovatio.llm.residual.ResidualAnnotationContext;
import org.shark.renovatio.profile.MigrationProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureSuggestionCoordinatorTest {
    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void onlyArchitectureUncertaintyConsumesTheProfileBoundedSuggestionBudget() {
        List<DecisionPoint> catalog = F1DecisionCatalog.create("a".repeat(64), NOW);
        DecisionPoint architecture = lowConfidence(catalog.stream()
                .filter(value -> value.category() == DecisionPoint.Category.ARCHITECTURE).findFirst().orElseThrow());
        DecisionPoint naming = lowConfidence(catalog.stream()
                .filter(value -> value.category() == DecisionPoint.Category.NAMING).findFirst().orElseThrow());
        AtomicInteger calls = new AtomicInteger();
        DecisionSuggestionService service = new DecisionSuggestionService((prompt, input, fallback) -> {
            calls.incrementAndGet();
            var output = JSON.createObjectNode();
            output.put("chosenOption", architecture.options().get(1));
            output.put("confidence", 0.72);
            output.put("rationale", "Bounded architecture recommendation.");
            return DecisionSuggestionService.RuntimeResult.success(output, false);
        });
        ArchitectureSuggestionCoordinator coordinator = coordinator(service);

        var batch = coordinator.suggest(List.of(naming, architecture), "b".repeat(64),
                new MigrationProfile.Llm(true, true, 1), NOW.plusSeconds(1));

        assertEquals(1, calls.get());
        assertEquals(1, batch.suggestionsAttempted());
        assertEquals(DecisionPoint.Status.AUTO, batch.decisions().get(0).status());
        assertEquals(DecisionPoint.Status.SUGGESTED, batch.decisions().get(1).status());
    }

    @Test
    void disabledPolicyMakesNoRuntimeCallAndControlFlowStillRequiresGreenEvidence() {
        AtomicInteger calls = new AtomicInteger();
        ArchitectureSuggestionCoordinator coordinator = coordinator(new DecisionSuggestionService(
                (prompt, input, fallback) -> {
                    calls.incrementAndGet();
                    return DecisionSuggestionService.RuntimeResult.failure(
                            DecisionPoint.LlmFailureCategory.PROVIDER_ERROR, false);
                }));
        DecisionPoint architecture = lowConfidence(F1DecisionCatalog.create("a".repeat(64), NOW).stream()
                .filter(value -> value.category() == DecisionPoint.Category.ARCHITECTURE).findFirst().orElseThrow());

        var skipped = coordinator.suggest(List.of(architecture), "b".repeat(64),
                new MigrationProfile.Llm(false, false, 0), NOW);
        assertEquals(0, calls.get());
        assertEquals(DecisionPoint.Status.AUTO, skipped.decisions().get(0).status());

        var rejected = coordinator.gateControlFlowPlan(
                new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION, "cobol-ir.v1",
                        "a".repeat(64), List.of()), plan(),
                context(), new CharacterizationEvidence("repo://baseline", true, true, false));
        assertFalse(rejected.proposalRetained());
        assertEquals(ControlFlowPlanGate.DIAGNOSTIC, rejected.diagnosticCode());

        var accepted = coordinator.gateControlFlowPlan(rejected.sidecar(), plan(), context(),
                new CharacterizationEvidence("repo://baseline", true, true, true));
        assertTrue(accepted.proposalRetained());
    }

    private static ArchitectureSuggestionCoordinator coordinator(DecisionSuggestionService service) {
        return new ArchitectureSuggestionCoordinator(service,
                new ControlFlowPlanGate(new ResidualAnnotationAssembler()));
    }

    private static DecisionPoint lowConfidence(DecisionPoint value) {
        return new DecisionPoint(value.schemaVersion(), value.id(), value.category(), value.decisionKey(),
                value.location(), value.question(), value.options(), value.defaultOption(), value.chosenOption(),
                value.source(), new BigDecimal("0.5"), value.rationale(), value.evidence(), value.status(),
                value.semanticIrHash(), false, null, value.revision(), value.createdAt(), value.updatedAt(), true);
    }

    private static ResidualAnnotationContext context() {
        return new ResidualAnnotationContext("cobol-ir.v1", "a".repeat(64), "c".repeat(64),
                org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind.PARAGRAPH,
                "offline", "fake", "v1", "control-flow-plan.v1", "d".repeat(64),
                "tool-20260901t00000000000000z",
                org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance.CacheDisposition.MISS,
                0.7, "project:owner", List.of("c".repeat(64)), "repo://baseline", List.of(), false);
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode plan() {
        var plan = JSON.createObjectNode();
        plan.putArray("steps").add("retain branch");
        plan.putArray("risks").add("loop termination");
        return plan;
    }
}
