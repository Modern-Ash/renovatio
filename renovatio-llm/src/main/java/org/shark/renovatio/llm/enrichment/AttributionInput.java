package org.shark.renovatio.llm.enrichment;

/** Non-sensitive attribution fields known before the provider call. */
public record AttributionInput(
        String promptId, String provider, String model, String inputHash, String cacheKey,
        String schemaHash, String runtimeContractVersion) {
}
