package org.modernash.renovatio.llm.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;

/** Production Ollama chat transport using the native /api/chat endpoint. */
public final class OllamaHttpTransport implements OllamaTransport {
    static final int MAX_RESPONSE_BYTES = 1_048_576;
    private final HttpClient client;
    private final ObjectMapper json;

    public OllamaHttpTransport() {
        this(HttpClient.newBuilder().build(), new ObjectMapper());
    }

    OllamaHttpTransport(HttpClient client, ObjectMapper json) {
        this.client = client;
        this.json = json;
    }

    @Override
    public LlmResponse send(LlmRequest request, OllamaConfiguration configuration) {
        HttpRequest httpRequest = HttpRequest.newBuilder(configuration.endpoint())
                .timeout(configuration.timeout())
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body(request, configuration.model())))
                .build();
        try {
            HttpResponse<InputStream> response = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new ProviderException(classifyStatus(response.statusCode()));
                }
                return new LlmResponse("ollama", configuration.model(),
                        decodeContent(json, new String(readBounded(body), StandardCharsets.UTF_8)));
            }
        } catch (HttpTimeoutException exception) {
            throw new ProviderException(ProviderFailure.PROVIDER_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProviderException(ProviderFailure.PROVIDER_UNAVAILABLE);
        } catch (IOException exception) {
            throw new ProviderException(ProviderFailure.PROVIDER_UNAVAILABLE);
        }
    }

    String body(LlmRequest request, String model) {
        ObjectNode root = json.createObjectNode();
        root.put("model", model);
        root.put("stream", false);
        root.put("format", "json");
        ObjectNode options = root.putObject("options");
        options.put("temperature", 0);
        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", request.systemPrompt());
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
        if (responseBody == null || responseBody.getBytes(StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES) {
            throw new ProviderException(ProviderFailure.OUTPUT_MALFORMED);
        }
        try {
            JsonNode envelope = json.readTree(responseBody);
            String text = envelope.path("message").path("content").textValue();
            if (text == null) {
                text = envelope.path("response").textValue();
            }
            if (text == null) {
                throw new ProviderException(ProviderFailure.OUTPUT_MALFORMED);
            }
            return json.readTree(text);
        } catch (JsonProcessingException exception) {
            throw new ProviderException(ProviderFailure.OUTPUT_MALFORMED);
        }
    }

    static byte[] readBounded(InputStream input) throws IOException {
        byte[] body = input.readNBytes(MAX_RESPONSE_BYTES + 1);
        if (body.length > MAX_RESPONSE_BYTES) {
            throw new ProviderException(ProviderFailure.OUTPUT_MALFORMED);
        }
        return body;
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
