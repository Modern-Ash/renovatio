package org.shark.renovatio.llm.prompt;

import org.shark.renovatio.llm.cache.CacheIdentity;
import org.shark.renovatio.llm.provider.LlmRequest;

/** Catalog-derived request and cache identity that cannot drift independently. */
public record PreparedEnrichment(CacheIdentity identity, LlmRequest request,
                                 PromptDefinition definition) {
}
