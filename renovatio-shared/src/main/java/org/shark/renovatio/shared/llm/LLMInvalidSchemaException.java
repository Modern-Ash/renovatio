package org.shark.renovatio.shared.llm;

/**
 * Thrown when an LLM response doesn't match the expected schema.
 */
public class LLMInvalidSchemaException extends LLMProviderException {

    public LLMInvalidSchemaException(String message) {
        super(message, "INVALID_SCHEMA", false);
    }

    public LLMInvalidSchemaException(String message, Throwable cause) {
        super(message, "INVALID_SCHEMA", false, cause);
    }
}