package org.shark.renovatio.jcl.decision;

import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.DecisionSuggestionPort;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Routes only BATCH decisions through the governed option-only suggestion service. */
public final class BatchSuggestionAdapter {
    private final DecisionSuggestionPort suggestions;

    public BatchSuggestionAdapter(DecisionSuggestionPort suggestions) {
        this.suggestions = Objects.requireNonNull(suggestions);
    }

    public DecisionSuggestionPort.SuggestionBatch suggest(List<DecisionPoint> decisions,
                                                          String profileHash, int providerCallCap,
                                                          Instant now) {
        List<DecisionPoint> batch = decisions.stream()
                .filter(value -> value.category() == DecisionPoint.Category.BATCH).toList();
        return suggestions.suggest(batch, profileHash, providerCallCap, now);
    }
}
