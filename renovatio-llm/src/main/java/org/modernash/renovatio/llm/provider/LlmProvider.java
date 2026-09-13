package org.modernash.renovatio.llm.provider;

/** Provider-neutral boundary for governed enrichment. */
public interface LlmProvider {
    LlmResponse complete(LlmRequest request);
}
