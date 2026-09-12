package org.shark.renovatio.llm.domain;

/**
 * Thrown when the LLM service is unavailable (circuit breaker open).
 */
public class LLMServiceUnavailableException extends LLMProviderException {

    public LLMServiceUnavailableException(String message) {
        super(message, "SERVICE_UNAVAILABLE", true);
    }

    public LLMServiceUnavailableException(String message, Throwable cause) {
        super(message, "SERVICE_UNAVAILABLE", true, cause);
    }
}