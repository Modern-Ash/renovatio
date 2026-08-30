package org.shark.renovatio.provider.cobol.guardrail;

import java.util.Objects;

/** Result of an individual guardrail check. */
public record GateCheckResult(boolean passed, String diagnosticReference) {

    public GateCheckResult {
        Objects.requireNonNull(diagnosticReference, "diagnosticReference");
        if (diagnosticReference.isBlank()) {
            throw new IllegalArgumentException("diagnosticReference must not be blank");
        }
    }

    public static GateCheckResult passed(String diagnosticReference) {
        return new GateCheckResult(true, diagnosticReference);
    }

    public static GateCheckResult failed(String diagnosticReference) {
        return new GateCheckResult(false, diagnosticReference);
    }
}
