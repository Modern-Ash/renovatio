package org.shark.renovatio.llm.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderRuntimeTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void offlineFakeReturnsScriptedResponseWithoutNetwork() {
        LlmResponse expected = new LlmResponse("fake", "offline", JSON.createObjectNode().put("ok", true));
        OfflineFakeProvider fake = new OfflineFakeProvider(List.of(expected));

        assertEquals(expected, fake.complete(request()));
        assertEquals(1, fake.calls());
        assertEquals(ProviderFailure.PROVIDER_UNAVAILABLE,
                assertThrows(ProviderException.class, () -> fake.complete(request())).failure());
    }

    @Test
    void configurationUsesEnvironmentOverrideAndFailsClosedWhenMissing() {
        Properties properties = new Properties();
        properties.setProperty(AnthropicConfiguration.MODEL_PROPERTY, "property-model");
        AnthropicConfiguration configuration = AnthropicConfiguration.from(properties, Map.of(
                AnthropicConfiguration.API_KEY_ENV, "secret",
                AnthropicConfiguration.MODEL_ENV, "environment-model"));

        assertEquals("environment-model", configuration.model());
        assertEquals(Duration.ofSeconds(60), configuration.timeout());
        assertEquals(ProviderFailure.PROVIDER_CONFIGURATION_INVALID,
                assertThrows(ProviderException.class,
                        () -> AnthropicConfiguration.from(new Properties(), Map.of())).failure());
    }

    @Test
    void retriesOnlyRetryableFailuresWithInjectedJitterAndSleeper() {
        Queue<Object> outcomes = new ArrayDeque<>();
        outcomes.add(new ProviderException(ProviderFailure.PROVIDER_RATE_LIMIT));
        outcomes.add(new ProviderException(ProviderFailure.PROVIDER_SERVER_ERROR));
        LlmResponse response = new LlmResponse("anthropic", "model", JSON.createObjectNode().put("ok", true));
        outcomes.add(response);
        List<Duration> sleeps = new ArrayList<>();
        AnthropicTransport transport = (request, configuration) -> {
            Object outcome = outcomes.remove();
            if (outcome instanceof ProviderException exception) {
                throw exception;
            }
            return (LlmResponse) outcome;
        };
        AnthropicLlmProvider provider = new AnthropicLlmProvider(configuration(), transport,
                new RetryPolicy(), () -> 0.5, sleeps::add);

        assertEquals(response, provider.complete(request()));
        assertEquals(List.of(Duration.ofMillis(250), Duration.ofMillis(500)), sleeps);
    }

    @Test
    void doesNotRetryAuthenticationAndClassifiesHttpStatuses() {
        List<Duration> sleeps = new ArrayList<>();
        AnthropicLlmProvider provider = new AnthropicLlmProvider(configuration(),
                (request, configuration) -> {
                    throw new ProviderException(ProviderFailure.PROVIDER_AUTHENTICATION);
                }, new RetryPolicy(), () -> 0.5, sleeps::add);

        assertEquals(ProviderFailure.PROVIDER_AUTHENTICATION,
                assertThrows(ProviderException.class, () -> provider.complete(request())).failure());
        assertEquals(List.of(), sleeps);
        assertEquals(ProviderFailure.PROVIDER_AUTHENTICATION, AnthropicHttpTransport.classifyStatus(401));
        assertEquals(ProviderFailure.PROVIDER_RATE_LIMIT, AnthropicHttpTransport.classifyStatus(429));
        assertEquals(ProviderFailure.PROVIDER_SERVER_ERROR, AnthropicHttpTransport.classifyStatus(503));
        assertEquals(ProviderFailure.PROVIDER_REQUEST_REJECTED, AnthropicHttpTransport.classifyStatus(400));
    }

    @Test
    void retryPolicyEnforcesNormativeBounds() {
        RetryPolicy policy = new RetryPolicy();
        assertEquals(Duration.ZERO, policy.delayBeforeAttempt(2, () -> 0.0));
        assertEquals(Duration.ofMillis(500), policy.delayBeforeAttempt(2, () -> 0.999999));
        assertEquals(Duration.ofMillis(1000), policy.delayBeforeAttempt(3, () -> 0.999999));
        assertThrows(IllegalArgumentException.class, () -> policy.delayBeforeAttempt(1, () -> 0.5));
        assertThrows(IllegalArgumentException.class, () -> policy.delayBeforeAttempt(2, () -> 1.0));
    }

    @Test
    void malformedAndOversizedClaudeBodiesFailWithStableCategory() {
        assertEquals(ProviderFailure.OUTPUT_MALFORMED,
                assertThrows(ProviderException.class,
                        () -> AnthropicHttpTransport.decodeContent(JSON, "not-json")).failure());
        assertEquals(ProviderFailure.OUTPUT_MALFORMED,
                assertThrows(ProviderException.class,
                        () -> AnthropicHttpTransport.decodeContent(JSON, "{}" )).failure());
        assertEquals(ProviderFailure.OUTPUT_MALFORMED,
                assertThrows(ProviderException.class,
                        () -> AnthropicHttpTransport.decodeContent(JSON,
                                "x".repeat(AnthropicHttpTransport.MAX_RESPONSE_BYTES + 1))).failure());
        assertEquals("value", AnthropicHttpTransport.decodeContent(JSON,
                "{\"content\":[{\"text\":\"{\\\"result\\\":\\\"value\\\"}\"}]}" )
                .path("result").textValue());
    }

    @Test
    void anthropicRequestEnforcesTemperatureZeroAndDeterministicPolicy() throws Exception {
        JsonNode body = JSON.readTree(new AnthropicHttpTransport().body(request(), "model"));

        assertEquals("model", body.path("model").textValue());
        assertEquals(4096, body.path("max_tokens").intValue());
        assertEquals(0, body.path("temperature").intValue());
        assertEquals("Return JSON", body.path("system").textValue());
        assertEquals(3, body.path("messages").size());
    }

    private static LlmRequest request() {
        return new LlmRequest("sample.v1", "Return JSON",
                List.of(new LlmRequest.Example(JSON.createObjectNode().put("sample", "input"),
                        JSON.createObjectNode().put("sample", "output"))),
                JSON.createObjectNode().put("input", "value"));
    }

    private static AnthropicConfiguration configuration() {
        return new AnthropicConfiguration("secret", "model", URI.create("https://example.invalid"),
                Duration.ofSeconds(60));
    }
}
