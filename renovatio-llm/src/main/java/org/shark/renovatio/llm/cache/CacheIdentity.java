package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

/** Complete identity inputs for one enrichment request. */
public record CacheIdentity(
        JsonNode canonicalInput,
        String promptId,
        String outputSchemaId,
        String outputSchemaHash,
        List<String> validators,
        String provider,
        String model) {

    public static final String IDENTITY_TYPE = "renovatio.llm.cache-request";
    public static final int IDENTITY_VERSION = 1;
    public static final String RUNTIME_CONTRACT_VERSION = "renovatio-llm.v1";

    public CacheIdentity {
        Objects.requireNonNull(canonicalInput, "canonicalInput");
        requireText(promptId, "promptId");
        requireText(outputSchemaId, "outputSchemaId");
        requireHash(outputSchemaHash, "outputSchemaHash");
        validators = List.copyOf(Objects.requireNonNull(validators, "validators"));
        if (validators.isEmpty()) throw new IllegalArgumentException("validators must not be empty");
        requireText(provider, "provider");
        requireText(model, "model");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }

    private static void requireHash(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
    }
}
