package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.annotation.JsonValue;

/** The immutable admission order for modernization proposals. */
public enum GuardrailGate {
    SCHEMA("schema"),
    COMPILATION("compilation"),
    CHARACTERIZATION("characterization"),
    REVIEW_ELIGIBILITY("review-eligibility");

    private final String externalName;

    GuardrailGate(String externalName) {
        this.externalName = externalName;
    }

    @JsonValue
    public String externalName() {
        return externalName;
    }
}
