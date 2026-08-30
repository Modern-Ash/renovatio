package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ManualActionReviewStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    RESOLVED("resolved");

    private final String externalName;

    ManualActionReviewStatus(String externalName) {
        this.externalName = externalName;
    }

    @JsonValue
    public String externalName() {
        return externalName;
    }
}
