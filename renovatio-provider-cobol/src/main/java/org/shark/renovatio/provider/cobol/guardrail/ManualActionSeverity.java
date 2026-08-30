package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ManualActionSeverity {
    WARNING("warning"),
    ERROR("error"),
    CRITICAL("critical");

    private final String externalName;

    ManualActionSeverity(String externalName) {
        this.externalName = externalName;
    }

    @JsonValue
    public String externalName() {
        return externalName;
    }
}
