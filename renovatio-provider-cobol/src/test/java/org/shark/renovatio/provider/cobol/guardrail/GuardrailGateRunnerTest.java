package org.shark.renovatio.provider.cobol.guardrail;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardrailGateRunnerTest {

    @Test
    void executesSuccessfulChecksInRequiredOrder() {
        List<GuardrailGate> observed = new ArrayList<>();
        GuardrailRunResult result = new GuardrailGateRunner(checks(observed, null)).run();

        assertThat(result.eligible()).isTrue();
        assertThat(result.executedGates()).containsExactly(GuardrailGate.values());
        assertThat(observed).containsExactly(GuardrailGate.values());
    }

    @Test
    void stopsAtEveryPossibleFirstFailure() {
        for (GuardrailGate failedGate : GuardrailGate.values()) {
            List<GuardrailGate> observed = new ArrayList<>();
            GuardrailRunResult result = new GuardrailGateRunner(checks(observed, failedGate)).run();

            assertThat(result.eligible()).isFalse();
            assertThat(result.failedGate()).isEqualTo(failedGate);
            assertThat(result.diagnosticReference()).isEqualTo("failed:" + failedGate.externalName());
            assertThat(observed).containsExactly(
                    List.of(GuardrailGate.values()).subList(0, failedGate.ordinal() + 1)
                            .toArray(GuardrailGate[]::new));
        }
    }

    @Test
    void rejectsIncompleteGateConfiguration() {
        assertThatThrownBy(() -> new GuardrailGateRunner(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("every guardrail gate");
    }

    private static Map<GuardrailGate, GateCheck> checks(
            List<GuardrailGate> observed, GuardrailGate failedGate) {
        EnumMap<GuardrailGate, GateCheck> checks = new EnumMap<>(GuardrailGate.class);
        for (GuardrailGate gate : GuardrailGate.values()) {
            checks.put(gate, () -> {
                observed.add(gate);
                return gate == failedGate
                        ? GateCheckResult.failed("failed:" + gate.externalName())
                        : GateCheckResult.passed("passed:" + gate.externalName());
            });
        }
        return checks;
    }
}
