package org.shark.renovatio.llm.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.shark.renovatio.llm.provider.ProviderFailure;

import java.io.IOException;

/** Renders deterministic fallback metadata from the prompt's versioned catalog resource. */
public final class CatalogFallbackFactory {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private final PromptRuntime runtime;

    public CatalogFallbackFactory(PromptRuntime runtime) {
        this.runtime = runtime;
    }

    public JsonNode create(PromptDefinition definition, ProviderFailure failure, JsonNode deterministicResult) {
        try {
            JsonNode template = YAML.readTree(runtime.resource(definition.fallback()));
            return YAML.createObjectNode()
                    .put("diagnosticCode", failure.diagnosticCode())
                    .put("manualAction", template.path("manualAction").asText())
                    .set("deterministicResult", deterministicResult.deepCopy());
        } catch (IOException exception) {
            throw new PromptCatalogException("PROMPT_FALLBACK_INVALID", definition.fallback());
        }
    }
}
