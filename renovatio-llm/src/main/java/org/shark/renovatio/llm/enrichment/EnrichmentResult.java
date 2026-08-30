package org.shark.renovatio.llm.enrichment;

import org.shark.renovatio.llm.cache.CacheEnvelope;

public record EnrichmentResult(CacheEnvelope envelope, boolean cacheHit) {
}
