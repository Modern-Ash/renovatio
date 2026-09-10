package org.shark.renovatio.provider.cobol.guardrail;

import java.util.Map;
import java.util.Set;

/** Inputs needed to decide whether a bounded proposal is eligible for human review. */
public record ReviewEligibilityRequest(
        ProposalManifest manifest,
        Set<String> changedPaths,
        Set<String> publicSignatureChanges,
        Set<String> ownerApprovedSignatures,
        Map<String, String> provenance,
        boolean byteReproducible) {
}
