package org.shark.renovatio.llm.decision;

import com.fasterxml.jackson.databind.JsonNode;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.llm.residual.CharacterizationEvidence;
import org.shark.renovatio.llm.residual.ControlFlowPlanGate;
import org.shark.renovatio.llm.residual.ResidualAnnotationContext;
import org.shark.renovatio.profile.MigrationProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Bounded adapter for optional architecture suggestions and gated control-flow plans. */
public final class ArchitectureSuggestionCoordinator implements ArchitectureSuggestionGateway {
    private final DecisionSuggestionService suggestions;
    private final ControlFlowPlanGate controlFlowPlans;

    public ArchitectureSuggestionCoordinator(DecisionSuggestionService suggestions,
                                             ControlFlowPlanGate controlFlowPlans) {
        this.suggestions = Objects.requireNonNull(suggestions, "suggestions");
        this.controlFlowPlans = Objects.requireNonNull(controlFlowPlans, "controlFlowPlans");
    }

    @Override
    public DecisionSuggestionService.SuggestionBatch suggest(List<DecisionPoint> current, String profileHash,
                                                              MigrationProfile.Llm policy, Instant now) {
        List<DecisionPoint> decisions = List.copyOf(Objects.requireNonNull(current, "current"));
        Objects.requireNonNull(policy, "policy");
        if (!Boolean.TRUE.equals(policy.enabled()) || !Boolean.TRUE.equals(policy.suggestDecisions())
                || policy.maxSuggestionsPerRun() == null || policy.maxSuggestionsPerRun() == 0) {
            return new DecisionSuggestionService.SuggestionBatch(decisions, 0, 0, 0);
        }
        List<DecisionPoint> architecture = decisions.stream()
                .filter(value -> value.category() == DecisionPoint.Category.ARCHITECTURE).toList();
        DecisionSuggestionService.SuggestionBatch evaluated = suggestions.suggest(architecture, profileHash,
                policy.maxSuggestionsPerRun(), now);
        Map<String, DecisionPoint> replacements = new LinkedHashMap<>();
        evaluated.decisions().forEach(value -> replacements.put(value.id(), value));
        List<DecisionPoint> merged = new ArrayList<>(decisions.size());
        decisions.forEach(value -> merged.add(replacements.getOrDefault(value.id(), value)));
        return new DecisionSuggestionService.SuggestionBatch(List.copyOf(merged), evaluated.suggestionsAttempted(),
                evaluated.suggestionsFailed(), evaluated.cacheHits());
    }

    public ControlFlowPlanGate.Decision gateControlFlowPlan(AnnotatedCobolModel sidecar, JsonNode validatedPlan,
                                                             ResidualAnnotationContext context,
                                                             CharacterizationEvidence evidence) {
        return controlFlowPlans.retainIfEligible(sidecar, validatedPlan, context, evidence);
    }
}
