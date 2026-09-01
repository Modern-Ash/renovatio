package org.shark.renovatio.llm.decision;

import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.profile.MigrationProfile;

import java.time.Instant;
import java.util.List;

/** Application boundary for optional, governed architecture suggestions. */
public interface ArchitectureSuggestionGateway {
    DecisionSuggestionService.SuggestionBatch suggest(List<DecisionPoint> current, String profileHash,
                                                       MigrationProfile.Llm policy, Instant now);
}
