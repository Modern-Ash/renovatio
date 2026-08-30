package org.shark.renovatio.provider.cobol.guardrail;

import java.util.Objects;

/** A bounded, reviewable record emitted when modernization fails closed. */
public record ManualActionItem(
        String id,
        String sourceFile,
        String program,
        String division,
        String section,
        String paragraph,
        String sourceSpan,
        String irNodeId,
        String sourceContentHash,
        String constructionFamily,
        String reason,
        GuardrailGate failedGate,
        String diagnosticReference,
        String fallback,
        String requiredHumanAction,
        String acceptanceCondition,
        ManualActionSeverity severity,
        ManualActionReviewStatus reviewStatus,
        String schemaHash,
        String promptHash,
        String modelId,
        String cacheHash,
        String outputHash,
        String agoraToolRun) implements Comparable<ManualActionItem> {

    public ManualActionItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(constructionFamily, "constructionFamily");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(failedGate, "failedGate");
        Objects.requireNonNull(diagnosticReference, "diagnosticReference");
        Objects.requireNonNull(fallback, "fallback");
        Objects.requireNonNull(requiredHumanAction, "requiredHumanAction");
        Objects.requireNonNull(acceptanceCondition, "acceptanceCondition");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(reviewStatus, "reviewStatus");
    }

    @Override
    public int compareTo(ManualActionItem other) {
        return id.compareTo(other.id);
    }
}
