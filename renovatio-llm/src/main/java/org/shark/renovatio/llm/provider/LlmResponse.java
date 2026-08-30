package org.shark.renovatio.llm.provider;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/** Parsed response content without a raw provider envelope. */
public record LlmResponse(String provider, String model, JsonNode content) {
    public LlmResponse {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(content, "content");
    }
}
