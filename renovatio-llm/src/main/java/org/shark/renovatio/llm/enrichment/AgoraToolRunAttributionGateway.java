package org.shark.renovatio.llm.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Production attribution boundary backed by the currently running Agora Tool Run. */
public final class AgoraToolRunAttributionGateway implements AttributionGateway {
    public static final String TOOL_RUN_ENV = "AGORA_TOOL_RUN";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private final Map<String, String> environment;
    private final Consumer<AttributionResult> resultSink;

    public AgoraToolRunAttributionGateway(Map<String, String> environment,
                                          Consumer<AttributionResult> resultSink) {
        this.environment = Map.copyOf(environment);
        this.resultSink = Objects.requireNonNull(resultSink);
    }

    @Override
    public String begin(AttributionInput input) {
        String location = environment.get(TOOL_RUN_ENV);
        if (location == null || location.isBlank()) throw new AttributionException(AttributionException.Stage.INIT);
        Path record = Path.of(location).toAbsolutePath().normalize();
        try {
            String document = Files.readString(record);
            int close = document.indexOf("\n---", 4);
            if (!document.startsWith("---\n") || close < 0) throw new IOException();
            JsonNode metadata = YAML.readTree(document.substring(4, close));
            JsonNode inputs = metadata.path("inputs");
            require(metadata.path("tool").asText().equals("llm-enrichment"));
            require(metadata.path("operation").asText().equals("enrich"));
            require(metadata.path("status").asText().equals("running"));
            require(inputs.path("prompt-id").asText().equals(input.promptId()));
            require(inputs.path("provider").asText().equals(input.provider()));
            require(inputs.path("model").asText().equals(input.model()));
            require(inputs.path("input-hash").asText().equals(input.inputHash()));
            require(inputs.path("cache-key").asText().equals(input.cacheKey()));
            require(inputs.path("schema-hash").asText().equals(input.schemaHash()));
            require(inputs.path("runtime-contract-version").asText().equals(input.runtimeContractVersion()));
            String id = metadata.path("id").asText();
            require(id.matches("tool-[a-z0-9]+"));
            return id;
        } catch (IOException | RuntimeException exception) {
            throw new AttributionException(AttributionException.Stage.INIT);
        }
    }

    @Override
    public void complete(String runReference, AttributionResult result) {
        if (runReference == null || !runReference.matches("tool-[a-z0-9]+")) {
            throw new AttributionException(AttributionException.Stage.FINALIZE);
        }
        try {
            resultSink.accept(result);
        } catch (RuntimeException exception) {
            throw new AttributionException(AttributionException.Stage.FINALIZE);
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalStateException();
    }
}
