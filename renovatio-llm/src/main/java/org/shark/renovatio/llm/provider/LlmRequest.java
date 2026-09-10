package org.shark.renovatio.llm.provider;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.List;

/** Immutable provider-neutral request. */
public record LlmRequest(String promptId, String systemPrompt, List<Example> fewShot, JsonNode input) {
    public LlmRequest {
        requireText(promptId, "promptId");
        requireText(systemPrompt, "systemPrompt");
        fewShot = List.copyOf(Objects.requireNonNull(fewShot, "fewShot"));
        if (fewShot.isEmpty()) throw new IllegalArgumentException("fewShot is required");
        Objects.requireNonNull(input, "input");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    public record Example(JsonNode input, JsonNode output) {
        public Example {
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(output, "output");
        }
    }
}
