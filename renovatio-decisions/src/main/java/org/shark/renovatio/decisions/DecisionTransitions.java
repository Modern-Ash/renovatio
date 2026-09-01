package org.shark.renovatio.decisions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.shark.renovatio.decisions.DecisionPoint.*;

/** Pure state transitions shared by API and persistence adapters. */
public final class DecisionTransitions {
    private DecisionTransitions() { }

    public static DecisionPoint patch(DecisionPoint current, String option, long expectedRevision, Instant now) {
        if (expectedRevision != current.revision()) throw new StaleDecisionException();
        if (option == null || !current.options().contains(option)) throw new InvalidOptionException();
        if (option.equals(current.chosenOption())) {
            if (current.status() == Status.CONFIRMED || current.status() == Status.OVERRIDDEN) return current;
            return copy(current, option, current.source(), current.confidence(), current.rationale(),
                    Status.CONFIRMED, current.llmFailed(), current.llmFailureCategory(),
                    current.revision() + 1, now, current.active());
        }
        return copy(current, option, Source.USER, BigDecimal.ONE,
                "User selected an allowed alternative.", Status.OVERRIDDEN, false, null,
                current.revision() + 1, now, current.active());
    }

    public static DecisionPoint suggest(DecisionPoint current, String option, BigDecimal confidence,
                                        String rationale, Instant now) {
        if (!current.options().contains(option)) throw new InvalidOptionException();
        if (current.status() == Status.CONFIRMED || current.status() == Status.OVERRIDDEN) return current;
        return copy(current, option, Source.LLM, confidence, rationale, Status.SUGGESTED,
                false, null, current.revision() + 1, now, current.active());
    }

    public static DecisionPoint llmFailure(DecisionPoint current, LlmFailureCategory category, Instant now) {
        if (current.status() == Status.CONFIRMED || current.status() == Status.OVERRIDDEN) return current;
        return copy(current, current.defaultOption(), Source.HEURISTIC, BigDecimal.ONE,
                current.rationale(), Status.AUTO, true, category, current.revision() + 1, now, current.active());
    }

    public static DecisionPoint reconcile(DecisionPoint current, DecisionPoint heuristic, Instant now) {
        if ((current.status() == Status.CONFIRMED || current.status() == Status.OVERRIDDEN)
                && heuristic.options().contains(current.chosenOption())) {
            return new DecisionPoint(current.schemaVersion(), current.id(), heuristic.category(),
                    heuristic.decisionKey(), heuristic.location(), heuristic.question(), heuristic.options(),
                    heuristic.defaultOption(), current.chosenOption(), current.source(), current.confidence(),
                    current.rationale(), heuristic.evidence(), current.status(), heuristic.semanticIrHash(),
                    false, null, current.revision() + 1, current.createdAt(), now, true);
        }
        List<String> evidence = new ArrayList<>(heuristic.evidence());
        if (current.status() == Status.CONFIRMED || current.status() == Status.OVERRIDDEN)
            evidence.add("PREVIOUS_CHOICE_INVALIDATED");
        return new DecisionPoint(heuristic.schemaVersion(), current.id(), heuristic.category(),
                heuristic.decisionKey(), heuristic.location(), heuristic.question(), heuristic.options(),
                heuristic.defaultOption(), heuristic.defaultOption(), Source.HEURISTIC, BigDecimal.ONE,
                heuristic.rationale(), evidence, Status.AUTO, heuristic.semanticIrHash(), false, null,
                current.revision() + 1, current.createdAt(), now, true);
    }

    public static DecisionPoint retire(DecisionPoint current, Instant now) {
        if (!current.active()) return current;
        return copy(current, current.chosenOption(), current.source(), current.confidence(), current.rationale(),
                current.status(), current.llmFailed(), current.llmFailureCategory(),
                current.revision() + 1, now, false);
    }

    public static BulkResult bulkConfirm(List<DecisionPoint> decisions, BigDecimal threshold, Instant now) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.ONE) > 0)
            throw new IllegalArgumentException("minConfidence must be between 0 and 1");
        List<DecisionPoint> changed = decisions.stream().filter(DecisionPoint::active)
                .filter(value -> value.status() == Status.AUTO || value.status() == Status.SUGGESTED)
                .filter(value -> value.confidence().compareTo(threshold) >= 0)
                .sorted(Comparator.comparing(DecisionPoint::id))
                .map(value -> patch(value, value.chosenOption(), value.revision(), now)).toList();
        return new BulkResult(changed.size(), decisions.size() - changed.size(), changed);
    }

    private static DecisionPoint copy(DecisionPoint value, String option, Source source,
                                      BigDecimal confidence, String rationale, Status status,
                                      boolean failed, LlmFailureCategory failure, long revision,
                                      Instant updatedAt, boolean active) {
        return new DecisionPoint(value.schemaVersion(), value.id(), value.category(), value.decisionKey(),
                value.location(), value.question(), value.options(), value.defaultOption(), option, source,
                confidence, rationale, value.evidence(), status, value.semanticIrHash(), failed, failure,
                revision, value.createdAt(), updatedAt, active);
    }

    public record BulkResult(int confirmed, int skipped, List<DecisionPoint> items) { }
    public static final class StaleDecisionException extends IllegalArgumentException { }
    public static final class InvalidOptionException extends IllegalArgumentException { }
}
