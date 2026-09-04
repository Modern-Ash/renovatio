package org.shark.renovatio.jcl.decision;

import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.llm.decision.DecisionSuggestionService;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Routes only BATCH decisions through the governed option-only suggestion service. */
public final class BatchSuggestionAdapter {
    private final DecisionSuggestionService suggestions;

    public BatchSuggestionAdapter(DecisionSuggestionService suggestions) {
        this.suggestions = Objects.requireNonNull(suggestions);
    }

    public DecisionSuggestionService.SuggestionBatch suggest(List<DecisionPoint> decisions,
                                                             String profileHash, int providerCallCap,
                                                             Instant now) {
        List<DecisionPoint> batch = decisions.stream()
                .filter(value -> value.category() == DecisionPoint.Category.BATCH).toList();
        return suggestions.suggest(batch, profileHash, providerCallCap, now);
    }
}
