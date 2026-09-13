package org.modernash.renovatio.llm.enrichment;

import org.modernash.renovatio.llm.cache.CacheEnvelope;

public record EnrichmentResult(CacheEnvelope envelope, boolean cacheHit) {
}
