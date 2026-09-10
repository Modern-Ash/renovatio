package org.shark.renovatio.provider.cobol.guardrail;

/** One deterministic, side-effect-bounded admission check. */
@FunctionalInterface
public interface GateCheck {

    GateCheckResult execute();
}
