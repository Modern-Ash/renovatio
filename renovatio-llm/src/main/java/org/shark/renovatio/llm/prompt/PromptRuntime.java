package org.shark.renovatio.llm.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import org.shark.renovatio.llm.cache.CacheIdentity;
import org.shark.renovatio.llm.cache.CacheKey;
import org.shark.renovatio.llm.provider.LlmRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Resolves immutable catalog resources into one bound provider request and cache identity. */
public final class PromptRuntime {
    private final PromptCatalog catalog;
    private final ClassLoader resources;

    public PromptRuntime(PromptCatalog catalog) {
        this(catalog, PromptRuntime.class.getClassLoader());
    }

    PromptRuntime(PromptCatalog catalog, ClassLoader resources) {
        this.catalog = catalog;
        this.resources = resources;
    }

    public PreparedEnrichment prepare(String promptId, JsonNode canonicalInput,
                                      String provider, String model) {
        PromptDefinition definition = catalog.require(promptId);
        byte[] schema = resource(definition.outputSchema());
        List<LlmRequest.Example> examples = definition.fewShot().stream()
                .map(example -> new LlmRequest.Example(example.input(), example.output())).toList();
        LlmRequest request = new LlmRequest(promptId, definition.system(), examples, canonicalInput);
        CacheIdentity identity = new CacheIdentity(canonicalInput, promptId, definition.outputSchema(),
                CacheKey.sha256(schema), definition.validators(), provider, model);
        return new PreparedEnrichment(identity, request, definition);
    }

    public byte[] resource(String path) {
        try (InputStream input = resources.getResourceAsStream(path)) {
            if (input == null) throw new PromptCatalogException("PROMPT_RESOURCE_MISSING", path);
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new PromptCatalogException("PROMPT_RESOURCE_UNREADABLE", path);
        }
    }
}
