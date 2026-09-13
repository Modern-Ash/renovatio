package org.shark.renovatio.shared.llm;

/**
 * Interface for LLM providers. Implementations must be thread-safe.
 * The provider only generates proposals - it never writes files, executes code, or makes decisions.
 */
public interface LLMProvider {

    /**
     * Generates a typed proposal for the given request.
     *
     * @param request the proposal request with input, budget, and model constraints
     * @return a typed proposal validated against the expected schema
     * @throws LLMProviderException if the provider fails
     * @throws LLMTimeoutException if the request exceeds the configured timeout
     * @throws LLMBudgetExceededException if the request exceeds budget limits
     * @throws LLMInvalidSchemaException if the response doesn't match the expected schema
     */
    TypedProposal propose(ProposalRequest request);

    /**
     * Returns metadata about this provider for governance and debugging.
     */
    ProposalMetadata metadata();

    /**
     * Configures the provider at runtime.
     */
    void configure(LLMConfig config);

    /**
     * Checks if the provider is healthy and available.
     */
    boolean isHealthy();
}