package org.shark.renovatio.shared.llm;

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