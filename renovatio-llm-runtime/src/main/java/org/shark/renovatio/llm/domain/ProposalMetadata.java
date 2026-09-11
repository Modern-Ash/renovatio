package org.shark.renovatio.llm.domain;

import java.util.List;

/**
 * Metadata about an LLM provider for governance and debugging.
 */
public record ProposalMetadata(
    String providerName,
    String providerVersion,
    String modelName,
    String modelVersion,
    boolean isOffline,
    List<String> supportedKinds
) {}