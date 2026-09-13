package org.shark.renovatio.decisions;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Stable proposal boundary for deterministic domains that may ask for governed suggestions. */
public interface DecisionSuggestionPort {
    SuggestionBatch suggest(List<DecisionPoint> current, String profileHash, int providerCallCap, Instant now);

    static String promptId(DecisionPoint.Category category) {
        return "decision." + category.name().toLowerCase(Locale.ROOT).replace('_', '-') + ".v1";
    }

    record SuggestionBatch(List<DecisionPoint> decisions, int suggestionsAttempted,
                           int suggestionsFailed, int cacheHits) { }
}
