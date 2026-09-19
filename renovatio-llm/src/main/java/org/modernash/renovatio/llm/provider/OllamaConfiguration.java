package org.modernash.renovatio.llm.provider;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/** Validated Ollama runtime configuration for local, OpenAI-free enrichment. */
public record OllamaConfiguration(String model, URI endpoint, Duration timeout) {
    public static final String MODEL_PROPERTY = "renovatio.llm.ollama.model";
    public static final String ENDPOINT_PROPERTY = "renovatio.llm.ollama.endpoint";
    public static final String MODEL_ENV = "RENOVATIO_LLM_OLLAMA_MODEL";
    public static final String ENDPOINT_ENV = "RENOVATIO_LLM_OLLAMA_ENDPOINT";
    private static final URI DEFAULT_ENDPOINT = URI.create("http://localhost:11434/api/chat");

    public OllamaConfiguration {
        if (model == null || model.isBlank() || endpoint == null
                || timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new ProviderException(ProviderFailure.PROVIDER_CONFIGURATION_INVALID);
        }
    }

    public static OllamaConfiguration from(Properties properties, Map<String, String> environment) {
        String model = environment.getOrDefault(MODEL_ENV, properties.getProperty(MODEL_PROPERTY));
        String endpoint = environment.getOrDefault(ENDPOINT_ENV,
                properties.getProperty(ENDPOINT_PROPERTY, DEFAULT_ENDPOINT.toString()));
        return new OllamaConfiguration(model, URI.create(endpoint), Duration.ofSeconds(120));
    }
}
