package org.shark.renovatio.llm.enrichment;

/** Non-sensitive fields that finalize one governed cache miss. */
public record AttributionResult(
        String outputHash, String resultDisposition, String promotionDisposition,
        String failureCategory, String artifactUri, String schemaHash, String cacheKey,
        String envelopeHash, String runtimeContractVersion) {
}
