package org.shark.renovatio.llm.domain;

/**
 * Thrown when an LLM request exceeds budget limits.
 */
public class LLMBudgetExceededException extends LLMProviderException {

    public LLMBudgetExceededException(String message) {
        super(message, "BUDGET_EXCEEDED", false);
    }

    public LLMBudgetExceededException(String message, Throwable cause) {
        super(message, "BUDGET_EXCEEDED", false, cause);
    }
}