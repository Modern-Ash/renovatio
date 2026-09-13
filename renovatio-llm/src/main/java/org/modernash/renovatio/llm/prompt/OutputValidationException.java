package org.modernash.renovatio.llm.prompt;

import org.modernash.renovatio.llm.provider.ProviderFailure;

/** Stable validation rejection without model-authored diagnostics. */
public final class OutputValidationException extends RuntimeException {
    private final ProviderFailure failure;

    public OutputValidationException(ProviderFailure failure) {
        super(failure.diagnosticCode());
        this.failure = failure;
    }

    public ProviderFailure failure() {
        return failure;
    }
}
