package org.shark.renovatio.provider.cobol.guardrail;

import java.util.List;

/** Ordered trace of a proposal admission attempt. */
public record GuardrailRunResult(
        boolean eligible,
        GuardrailGate failedGate,
        String diagnosticReference,
        List<GuardrailGate> executedGates) {

    public GuardrailRunResult {
        executedGates = List.copyOf(executedGates);
    }
}
