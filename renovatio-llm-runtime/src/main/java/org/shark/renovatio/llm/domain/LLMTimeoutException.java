package org.shark.renovatio.llm.domain;

/**
 * Thrown when an LLM request times out.
 */
public class LLMTimeoutException extends LLMProviderException {

    public LLMTimeoutException(String message) {
        super(message, "TIMEOUT", true);
    }

    public LLMTimeoutException(String message, Throwable cause) {
        super(message, "TIMEOUT", true, cause);
    }
}