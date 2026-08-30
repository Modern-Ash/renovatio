package org.shark.renovatio.llm.provider;

/** Sanitized provider error: it never contains payloads, credentials, or headers. */
public final class ProviderException extends RuntimeException {
    private final ProviderFailure failure;

    public ProviderException(ProviderFailure failure) {
        super(failure.diagnosticCode());
        this.failure = failure;
    }

    public ProviderFailure failure() {
        return failure;
    }
}
