package org.shark.renovatio.llm.prompt;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** Immutable, versioned prompt-catalog entry. */
public record PromptDefinition(
        String promptId,
        String appliesTo,
        String system,
        List<FewShotExample> fewShot,
        String outputSchema,
        List<String> validators,
        String fallback) {

    public PromptDefinition {
        fewShot = fewShot == null ? null : List.copyOf(fewShot);
        validators = validators == null ? null : List.copyOf(validators);
    }

    public record FewShotExample(JsonNode input, JsonNode output) {
    }
}
