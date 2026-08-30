package org.shark.renovatio.llm.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;
import org.shark.renovatio.llm.cache.CacheIdentity;
import org.shark.renovatio.llm.cache.CacheKey;
import org.shark.renovatio.llm.cache.CommittedCacheArtifacts;
import org.shark.renovatio.llm.cache.CommittedCacheArtifactsLoader;
import org.shark.renovatio.llm.cache.ContentAddressedCache;
import org.shark.renovatio.llm.cache.GitHeadRepositoryTree;
import org.shark.renovatio.llm.cache.GitPromotionRepository;
import org.shark.renovatio.llm.cache.GovernedPromotionVerifier;
import org.shark.renovatio.llm.enrichment.AgoraToolRunAttributionGateway;
import org.shark.renovatio.llm.enrichment.GovernedEnrichmentService;
import org.shark.renovatio.llm.enrichment.PersistenceSanitizer;
import org.shark.renovatio.llm.prompt.PreparedEnrichment;
import org.shark.renovatio.llm.prompt.PromptCatalogLoader;
import org.shark.renovatio.llm.prompt.PromptRuntime;
import org.shark.renovatio.llm.provider.AnthropicConfiguration;
import org.shark.renovatio.llm.provider.AnthropicHttpTransport;
import org.shark.renovatio.llm.provider.AnthropicLlmProvider;
import org.shark.renovatio.llm.provider.LlmProvider;
import org.shark.renovatio.llm.provider.LlmResponse;
import org.shark.renovatio.llm.provider.OfflineFakeProvider;
import org.shark.renovatio.llm.provider.RetryPolicy;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Executable boundary for one Agora-governed cache miss. */
public final class LlmEnrichmentCli {
    static final String REQUEST_ENV = "RENOVATIO_LLM_REQUEST";
    static final int MAX_REQUEST_BYTES = 1_048_576;
    private static final ObjectMapper JSON = new ObjectMapper();

    private LlmEnrichmentCli() { }

    public static void main(String[] args) {
        try {
            run(args, System.getenv(), System.getProperties(), System.out);
        } catch (RuntimeException exception) {
            System.err.println("renovatio-llm-enrich: governed execution failed");
            System.exit(1);
        }
    }

    static void run(String[] args, Map<String, String> environment, Properties properties, PrintStream output) {
        Map<String, String> options = parse(args);
        Request request = readRequest(environment.get(REQUEST_ENV));
        PromptRuntime runtime = new PromptRuntime(new PromptCatalogLoader().loadDefault());
        PreparedEnrichment prepared = runtime.prepare(options.get("prompt-id"), request.canonicalInput(),
                options.get("provider"), options.get("model"));
        verify(options, prepared);

        Path project = Path.of(environment.getOrDefault("AGORA_PROJECT", ".")).toAbsolutePath().normalize();
        ContentAddressedCache cache = new ContentAddressedCache(
                project.resolve("renovatio-llm/src/main/resources/llm-cache"),
                project.resolve("renovatio-llm/target/llm-cache-quarantine"));
        AgoraToolRunAttributionGateway gateway = new AgoraToolRunAttributionGateway(environment, result -> {
            try {
                output.println(JSON.writeValueAsString(result));
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
        CommittedCacheArtifacts authority = new CommittedCacheArtifactsLoader()
                .load(new GitHeadRepositoryTree(project));
        new GovernedPromotionVerifier().verify(new GitPromotionRepository(project), authority);
        new GovernedEnrichmentService(() -> provider(options, request, environment, properties),
                cache, gateway, new PersistenceSanitizer(), runtime)
                .enrich(options.get("prompt-id"), request.canonicalInput(), options.get("provider"),
                        options.get("model"), request.deterministicResult(), authority.index(),
                        authority.manifest());
    }

    private static LlmProvider provider(Map<String, String> options, Request request,
                                        Map<String, String> environment, Properties properties) {
        if ("offline-fake".equals(options.get("provider"))) {
            if (request.offlineResponse() == null || request.offlineResponse().isMissingNode()) {
                throw new IllegalArgumentException("offlineResponse is required for offline-fake");
            }
            return new OfflineFakeProvider(List.of(new LlmResponse("offline-fake", options.get("model"),
                    request.offlineResponse())));
        }
        if (!"anthropic".equals(options.get("provider"))) throw new IllegalArgumentException("provider");
        AnthropicConfiguration configuration = AnthropicConfiguration.from(properties, environment);
        if (!configuration.model().equals(options.get("model"))) throw new IllegalArgumentException("model");
        return new AnthropicLlmProvider(configuration, new AnthropicHttpTransport(), new RetryPolicy(),
                Math::random, delay -> sleep(delay));
    }

    private static void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static Request readRequest(String location) {
        if (location == null || location.isBlank()) throw new IllegalArgumentException(REQUEST_ENV);
        try {
            Path path = Path.of(location).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_REQUEST_BYTES) {
                throw new IllegalArgumentException("request");
            }
            JsonNode root = JSON.readTree(path.toFile());
            JsonNode input = root.path("canonicalInput");
            JsonNode deterministic = root.path("deterministicResult");
            if (!input.isObject() || !deterministic.isObject()) throw new IllegalArgumentException("request");
            return new Request(input, deterministic, root.path("offlineResponse"));
        } catch (IOException exception) {
            throw new IllegalArgumentException("request", exception);
        }
    }

    private static void verify(Map<String, String> options, PreparedEnrichment prepared) {
        CacheIdentity identity = prepared.identity();
        String inputHash = CacheKey.sha256(CanonicalJson.write(JSON.convertValue(
                identity.canonicalInput(), Object.class)));
        require(options.get("input-hash").equals(inputHash));
        require(options.get("cache-key").equals(CacheKey.derive(identity)));
        require(options.get("schema-hash").equals(identity.outputSchemaHash()));
        require(options.get("runtime-contract-version").equals(CacheIdentity.RUNTIME_CONTRACT_VERSION));
    }

    private static Map<String, String> parse(String[] args) {
        if (args.length != 15 || !"enrich".equals(args[0])) throw new IllegalArgumentException("arguments");
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index += 2) {
            if (!args[index].startsWith("--") || args[index + 1].isBlank()) {
                throw new IllegalArgumentException("arguments");
            }
            if (values.put(args[index].substring(2), args[index + 1]) != null) {
                throw new IllegalArgumentException("duplicate argument");
            }
        }
        for (String required : List.of("prompt-id", "provider", "model", "input-hash", "cache-key",
                "schema-hash", "runtime-contract-version")) {
            if (!values.containsKey(required)) throw new IllegalArgumentException(required);
        }
        return Map.copyOf(values);
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException("attribution identity mismatch");
    }

    private record Request(JsonNode canonicalInput, JsonNode deterministicResult, JsonNode offlineResponse) { }
}
