package org.modernash.renovatio.llm.decision;

import org.modernash.renovatio.decisions.DecisionPoint;
import org.modernash.renovatio.profile.MigrationProfile;

import java.time.Instant;
import java.util.List;

/** Application boundary for optional, governed architecture suggestions. */
public interface ArchitectureSuggestionGateway {
    DecisionSuggestionService.SuggestionBatch suggest(List<DecisionPoint> current, String profileHash,
                                                       MigrationProfile.Llm policy, Instant now);
}
