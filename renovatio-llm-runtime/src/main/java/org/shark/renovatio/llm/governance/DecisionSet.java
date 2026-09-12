package org.shark.renovatio.llm.governance;

import org.shark.renovatio.llm.domain.TypedProposal;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DecisionSet manages human review of LLM proposals.
 * Low confidence or high-risk proposals require explicit acceptance/rejection.
 * Rejection and fallback are explicit, reproducible states.
 */
@Component
public class DecisionSet {

    private final Map<String, DecisionRecord> decisions = new ConcurrentHashMap<>();

    /**
     * Submits a proposal for human review.
     * Returns a decision ID that can be used to accept/reject.
     */
    public String submitForReview(
            String decisionId,
            String proposalKind,
            TypedProposal proposal,
            String actor,
            String rationale) {

        DecisionRecord record = new DecisionRecord(
            decisionId,
            proposalKind,
            proposal,
            Instant.now(),
            actor,
            rationale
        );

        decisions.put(decisionId, record);
        return decisionId;
    }

    /**
     * Accepts a proposal.
     */
    public void accept(String decisionId, String actor, String rationale) {
        DecisionRecord record = decisions.get(decisionId);
        if (record == null) {
            throw new IllegalArgumentException("No pending decision for ID: " + decisionId);
        }
        record.setDecision(GovernanceLogger.Decision.ACCEPT);
        record.setDecidedBy(actor);
        record.setDecisionRationale(rationale);
        record.setDecidedAt(java.time.Instant.now());
    }

    /**
     * Rejects a proposal.
     */
    public void reject(String decisionId, String actor, String rationale) {
        DecisionRecord record = decisions.get(decisionId);
        if (record == null) {
            throw new IllegalArgumentException("No pending decision for ID: " + decisionId);
        }
        record.setDecision(GovernanceLogger.Decision.REJECT);
        record.setDecidedBy(actor);
        record.setDecisionRationale(rationale);
        record.setDecidedAt(java.time.Instant.now());
    }

    /**
     * Marks a proposal as fallback (using fake provider output).
     */
    public void fallback(String decisionId, String actor, String rationale) {
        DecisionRecord record = decisions.get(decisionId);
        if (record == null) {
            throw new IllegalArgumentException("No pending decision for ID: " + decisionId);
        }
        record.setDecision(GovernanceLogger.Decision.FALLBACK);
        record.setDecidedBy(actor);
        record.setDecisionRationale(rationale);
        record.setDecidedAt(java.time.Instant.now());
    }

    /**
     * Gets the decision record.
     */
    public DecisionRecord getDecision(String decisionId) {
        return decisions.get(decisionId);
    }

    /**
     * Checks if a decision is pending.
     */
    public boolean isPending(String decisionId) {
        DecisionRecord record = decisions.get(decisionId);
        return record != null && record.getDecision() == null;
    }

    /**
     * Gets all pending decisions for a given actor or all actors.
     */
    public java.util.List<DecisionRecord> getPendingDecisions(String actor) {
        return decisions.values().stream()
            .filter(r -> r.getDecision() == null)
            .filter(r -> actor == null || actor.equals(r.getSubmittedBy()))
            .toList();
    }

    /**
     * Record of a decision on a proposal.
     */
    public static class DecisionRecord {
        private final String decisionId;
        private final String proposalKind;
        private final TypedProposal proposal;
        private final java.time.Instant submittedAt;
        private final String submittedBy;
        private final String rationale;
        private GovernanceLogger.Decision decision;
        private String decidedBy;
        private String decisionRationale;
        private java.time.Instant decidedAt;

        public DecisionRecord(
                String decisionId,
                String proposalKind,
                TypedProposal proposal,
                java.time.Instant submittedAt,
                String submittedBy,
                String rationale) {
            this.decisionId = decisionId;
            this.proposalKind = proposalKind;
            this.proposal = proposal;
            this.submittedAt = submittedAt;
            this.submittedBy = submittedBy;
            this.rationale = rationale;
        }

        // Getters and setters
        public String getDecisionId() { return decisionId; }
        public String getProposalKind() { return proposalKind; }
        public TypedProposal getProposal() { return proposal; }
        public java.time.Instant getSubmittedAt() { return submittedAt; }
        public String getSubmittedBy() { return submittedBy; }
        public String getRationale() { return rationale; }
        public GovernanceLogger.Decision getDecision() { return decision; }
        public void setDecision(GovernanceLogger.Decision decision) { this.decision = decision; }
        public String getDecidedBy() { return decidedBy; }
        public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
        public String getDecisionRationale() { return decisionRationale; }
        public void setDecisionRationale(String decisionRationale) { this.decisionRationale = decisionRationale; }
        public java.time.Instant getDecidedAt() { return decidedAt; }
        public void setDecidedAt(java.time.Instant decidedAt) { this.decidedAt = decidedAt; }
    }
}