package org.shark.renovatio.llm.provider;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/** Validated Anthropic runtime configuration. */
public record AnthropicConfiguration(String apiKey, String model, URI endpoint, Duration timeout) {
    public static final String MODEL_PROPERTY = "renovatio.llm.anthropic.model";
    public static final String MODEL_ENV = "RENOVATIO_LLM_ANTHROPIC_MODEL";
    public static final String API_KEY_ENV = "ANTHROPIC_API_KEY";

    public AnthropicConfiguration {
        if (apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
            throw new ProviderException(ProviderFailure.PROVIDER_CONFIGURATION_INVALID);
        }
        if (endpoint == null || timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new ProviderException(ProviderFailure.PROVIDER_CONFIGURATION_INVALID);
        }
    }

    public static AnthropicConfiguration from(Properties properties, Map<String, String> environment) {
        String model = environment.getOrDefault(MODEL_ENV, properties.getProperty(MODEL_PROPERTY));
        return new AnthropicConfiguration(environment.get(API_KEY_ENV), model,
                URI.create("https://api.anthropic.com/v1/messages"), Duration.ofSeconds(60));
    }
}
