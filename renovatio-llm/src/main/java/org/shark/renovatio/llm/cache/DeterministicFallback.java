package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/** Non-model result retaining deterministic output plus a reviewable action item. */
public record DeterministicFallback(
        String failureCategory,
        String diagnosticCode,
        JsonNode deterministicResult,
        String manualAction) {

    public DeterministicFallback {
        requireText(failureCategory, "failureCategory");
        requireText(diagnosticCode, "diagnosticCode");
        Objects.requireNonNull(deterministicResult, "deterministicResult");
        requireText(manualAction, "manualAction");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
