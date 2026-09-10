package org.shark.renovatio.provider.cobol.guardrail;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Executes all admission gates in the non-negotiable order and fails fast. */
public final class GuardrailGateRunner {

    private static final List<GuardrailGate> ORDER = List.of(
            GuardrailGate.SCHEMA,
            GuardrailGate.COMPILATION,
            GuardrailGate.CHARACTERIZATION,
            GuardrailGate.REVIEW_ELIGIBILITY);

    private final Map<GuardrailGate, GateCheck> checks;

    public GuardrailGateRunner(Map<GuardrailGate, GateCheck> checks) {
        Objects.requireNonNull(checks, "checks");
        EnumMap<GuardrailGate, GateCheck> copy = new EnumMap<>(GuardrailGate.class);
        copy.putAll(checks);
        if (!copy.keySet().containsAll(ORDER)) {
            throw new IllegalArgumentException("A check is required for every guardrail gate");
        }
        this.checks = Map.copyOf(copy);
    }

    public GuardrailRunResult run() {
        List<GuardrailGate> executed = new ArrayList<>();
        for (GuardrailGate gate : ORDER) {
            executed.add(gate);
            GateCheckResult result = Objects.requireNonNull(
                    checks.get(gate).execute(), "Gate check returned null: " + gate.externalName());
            if (!result.passed()) {
                return new GuardrailRunResult(false, gate, result.diagnosticReference(), executed);
            }
        }
        return new GuardrailRunResult(true, null, "all-gates-passed", executed);
    }
}
