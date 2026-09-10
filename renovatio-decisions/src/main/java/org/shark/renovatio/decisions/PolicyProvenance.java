package org.shark.renovatio.decisions;

import org.shark.renovatio.profile.ReusableAssetIdentifier;

import java.math.BigDecimal;

/** Inspectable origin retained even after a local policy override. */
public record PolicyProvenance(
        String catalogName,
        String catalogVersion,
        String policyId,
        BigDecimal matchConfidence,
        String semanticSignature,
        boolean stale) {
    public PolicyProvenance {
        ReusableAssetIdentifier.require(catalogName, "catalogName");
        ReusableAssetIdentifier.require(catalogVersion, "catalogVersion");
        if (policyId == null || !policyId.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("policyId must be lowercase SHA-256");
        if (matchConfidence == null || matchConfidence.signum() < 0 || matchConfidence.compareTo(BigDecimal.ONE) > 0)
            throw new IllegalArgumentException("matchConfidence must be between 0 and 1");
        if (semanticSignature == null || !semanticSignature.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("semanticSignature must be lowercase SHA-256");
    }
}
