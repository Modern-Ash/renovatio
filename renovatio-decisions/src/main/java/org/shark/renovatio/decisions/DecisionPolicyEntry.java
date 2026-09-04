package org.shark.renovatio.decisions;

/** One reusable choice tied to a location-independent semantic signature. */
public record DecisionPolicyEntry(
        String policyId,
        SemanticDecisionSignature signature,
        String chosenOption,
        String sourceProjectId,
        String sourceDecisionId,
        long sourceDecisionRevision) {
    public DecisionPolicyEntry {
        if (policyId == null || !policyId.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("policyId must be lowercase SHA-256");
        if (signature == null) throw new IllegalArgumentException("signature is required");
        if (!signature.optionVocabulary().contains(chosenOption)) throw new IllegalArgumentException("chosenOption is not admitted");
        if (sourceProjectId == null || sourceProjectId.isBlank()) throw new IllegalArgumentException("sourceProjectId is required");
        if (sourceDecisionId == null || !sourceDecisionId.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("sourceDecisionId must be lowercase SHA-256");
        if (sourceDecisionRevision < 1) throw new IllegalArgumentException("sourceDecisionRevision must be positive");
    }

    public static DecisionPolicyEntry from(String projectId, DecisionPoint decision,
                                           SemanticDecisionSignature signature) {
        String id = org.shark.renovatio.profile.MigrationProfiles.sha256(
                signature.digest() + "\u0000" + decision.chosenOption());
        return new DecisionPolicyEntry(id, signature, decision.chosenOption(), projectId,
                decision.id(), decision.revision());
    }
}
