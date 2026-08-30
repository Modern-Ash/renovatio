package org.shark.renovatio.llm.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

/** Production Anthropic Messages transport using the JDK HTTP client. */
public final class AnthropicHttpTransport implements AnthropicTransport {
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    static final int MAX_RESPONSE_BYTES = 1_048_576;
    private final HttpClient client;
    private final ObjectMapper json;

    public AnthropicHttpTransport() {
        this(HttpClient.newBuilder().build(), new ObjectMapper());
    }

    AnthropicHttpTransport(HttpClient client, ObjectMapper json) {
        this.client = client;
        this.json = json;
    }

    @Override
    public LlmResponse send(LlmRequest request, AnthropicConfiguration configuration) {
        HttpRequest httpRequest = HttpRequest.newBuilder(configuration.endpoint())
                .timeout(configuration.timeout())
                .header("content-type", "application/json")
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("x-api-key", configuration.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body(request, configuration.model())))
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ProviderException(classifyStatus(response.statusCode()));
            }
            return new LlmResponse("anthropic", configuration.model(), decodeContent(json, response.body()));
        } catch (HttpTimeoutException exception) {
            throw new ProviderException(ProviderFailure.PROVIDER_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProviderException(ProviderFailure.PROVIDER_UNAVAILABLE);
        } catch (IOException exception) {
            throw new ProviderException(ProviderFailure.PROVIDER_UNAVAILABLE);
        }
    }

    private String body(LlmRequest request, String model) {
        ObjectNode root = json.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 4096);
        root.put("temperature", 0);
        root.put("system", request.systemPrompt());
        ArrayNode messages = root.putArray("messages");
        for (LlmRequest.Example example : request.fewShot()) {
            ObjectNode exampleInput = messages.addObject();
            exampleInput.put("role", "user");
            exampleInput.put("content", example.input().toString());
            ObjectNode exampleOutput = messages.addObject();
            exampleOutput.put("role", "assistant");
            exampleOutput.put("content", example.output().toString());
        }
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", request.input().toString());
        try {
            return json.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new ProviderException(ProviderFailure.PROVIDER_REQUEST_REJECTED);
        }
    }

    static JsonNode decodeContent(ObjectMapper json, String responseBody) {
        if (responseBody == null || responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                > MAX_RESPONSE_BYTES) {
            throw new ProviderException(ProviderFailure.OUTPUT_MALFORMED);
        }
        try {
            JsonNode envelope = json.readTree(responseBody);
            String text = envelope.path("content").path(0).path("text").textValue();
            if (text == null) {
                throw new ProviderException(ProviderFailure.OUTPUT_MALFORMED);
            }
            return json.readTree(text);
        } catch (JsonProcessingException exception) {
            throw new ProviderException(ProviderFailure.OUTPUT_MALFORMED);
        }
    }

    static ProviderFailure classifyStatus(int status) {
        if (status == 401 || status == 403) {
            return ProviderFailure.PROVIDER_AUTHENTICATION;
        }
        if (status == 429) {
            return ProviderFailure.PROVIDER_RATE_LIMIT;
        }
        if (status >= 500) {
            return ProviderFailure.PROVIDER_SERVER_ERROR;
        }
        if (status >= 400) {
            return ProviderFailure.PROVIDER_REQUEST_REJECTED;
        }
        return ProviderFailure.PROVIDER_UNAVAILABLE;
    }
}
