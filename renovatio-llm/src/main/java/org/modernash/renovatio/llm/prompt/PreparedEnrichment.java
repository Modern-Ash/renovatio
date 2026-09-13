package org.modernash.renovatio.llm.prompt;

import org.modernash.renovatio.llm.cache.CacheIdentity;
import org.modernash.renovatio.llm.provider.LlmRequest;

/** Catalog-derived request and cache identity that cannot drift independently. */
public record PreparedEnrichment(CacheIdentity identity, LlmRequest request,
                                 PromptDefinition definition) {
}
