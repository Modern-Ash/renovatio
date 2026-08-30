package org.shark.renovatio.llm.prompt;

/** Stable fail-closed catalog loading error. */
public final class PromptCatalogException extends RuntimeException {
    private final String code;

    PromptCatalogException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
