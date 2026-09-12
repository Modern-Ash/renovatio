package org.shark.renovatio.shared.llm;

/**
 * Base exception for LLM provider errors.
 */
public class LLMProviderException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;

    public LLMProviderException(String message, String errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public LLMProviderException(String message, String errorCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}