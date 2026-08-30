package org.shark.renovatio.llm.provider;

/** Closed provider failure vocabulary from the v1 contract. */
public enum ProviderFailure {
    PROVIDER_TIMEOUT(true),
    PROVIDER_RATE_LIMIT(true),
    PROVIDER_SERVER_ERROR(true),
    PROVIDER_AUTHENTICATION(false),
    PROVIDER_REQUEST_REJECTED(false),
    PROVIDER_CONFIGURATION_INVALID(false),
    PROVIDER_UNAVAILABLE(false),
    OUTPUT_MALFORMED(false),
    OUTPUT_SCHEMA_INVALID(false),
    VALIDATOR_REJECTED(false),
    SANITIZATION_REJECTED(false);

    private final boolean retryable;

    ProviderFailure(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }

    public String diagnosticCode() {
        return "LLM_" + name();
    }
}
