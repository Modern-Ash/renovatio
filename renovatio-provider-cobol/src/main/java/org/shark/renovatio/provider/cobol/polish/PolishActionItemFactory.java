package org.shark.renovatio.provider.cobol.polish;

import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItemIds;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionReviewStatus;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionSeverity;

final class PolishActionItemFactory {

    ManualActionItem create(PolishProposalRequest request, String proposalId,
                            GuardrailGate gate, String diagnostic, PolishCandidate candidate) {
        String nodeId = request.nodeSelectors().keySet().iterator().next();
        String reason = "Idiomatic polish " + request.family().name()
                + " failed at " + gate.externalName() + ": " + diagnostic;
        String id = ManualActionItemIds.from(
                request.sourceFile(), request.program(), proposalId, request.family().name(), reason);
        return new ManualActionItem(id, request.sourceFile(), request.program(), null, null, null,
                null, nodeId, request.sourceContentHash(), request.family().name(), reason, gate,
                "COBOL-POLISH-" + gate.name() + "-FAILED",
                "Deterministic generated Java retained unchanged",
                "Resolve the failed polish gate and request a new review-only proposal",
                "The same affected characterization selectors pass for a regenerated proposal",
                ManualActionSeverity.ERROR, ManualActionReviewStatus.PENDING,
                value(candidate, "outputSchemaHash"), value(candidate, "promptHash"),
                value(candidate, "modelId"), value(candidate, "cacheHash"),
                candidate == null ? null : PolishContracts.sha256(candidate.unifiedDiff()),
                value(candidate, "agoraToolRun"));
    }

    private String value(PolishCandidate candidate, String key) {
        return candidate == null ? null : candidate.provenance().get(key);
    }
}
