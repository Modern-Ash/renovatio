package org.shark.renovatio.llm.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.llm.cache.CacheKey;
import org.shark.renovatio.llm.cache.CommittedCacheIndex;
import org.shark.renovatio.llm.cache.ContentAddressedCache;
import org.shark.renovatio.llm.cache.ResultDisposition;
import org.shark.renovatio.llm.cache.CacheEnvelope;
import org.shark.renovatio.llm.cache.VerifiedPromotionManifest;
import org.shark.renovatio.llm.provider.LlmProvider;
import org.shark.renovatio.llm.provider.LlmResponse;
import org.shark.renovatio.llm.provider.ProviderException;
import org.shark.renovatio.llm.provider.ProviderFailure;
import org.shark.renovatio.llm.prompt.PromptCatalogLoader;
import org.shark.renovatio.llm.prompt.PromptRuntime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GovernedEnrichmentServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temporary;

    @Test
    void attributionInitializationFailurePreventsProviderCall() {
        AtomicInteger calls = new AtomicInteger();
        LlmProvider provider = request -> {
            calls.incrementAndGet();
            return success();
        };
        AttributionGateway gateway = new AttributionGateway() {
            @Override public String begin(AttributionInput input) { throw new IllegalStateException(); }
            @Override public void complete(String runReference, AttributionResult result) { }
        };

        AttributionException exception = assertThrows(AttributionException.class,
                () -> enrich(service(provider, gateway), emptyIndex()));

        assertEquals(AttributionException.Stage.INIT, exception.stage());
        assertEquals(0, calls.get());
        assertFalse(Files.exists(cacheRoot()));
    }

    @Test
    void providerConfigurationPreflightHappensBeforeAttributionInitialization() {
        AtomicInteger attributionCalls = new AtomicInteger();
        AttributionGateway gateway = new AttributionGateway() {
            @Override public String begin(AttributionInput input) {
                attributionCalls.incrementAndGet();
                return "tool-test";
            }
            @Override public void complete(String runReference, AttributionResult result) { }
        };
        GovernedEnrichmentService service = new GovernedEnrichmentService(() -> {
            throw new ProviderException(ProviderFailure.PROVIDER_CONFIGURATION_INVALID);
        }, cache(), gateway, new PersistenceSanitizer(), runtime());

        ProviderException exception = assertThrows(ProviderException.class,
                () -> enrich(service, emptyIndex()));

        assertEquals(ProviderFailure.PROVIDER_CONFIGURATION_INVALID, exception.failure());
        assertEquals(0, attributionCalls.get());
        assertFalse(Files.exists(cacheRoot()));
    }

    @Test
    void successfulMissIsAttributedAndWritesPendingCandidate() {
        CapturingGateway gateway = new CapturingGateway(false);
        EnrichmentResult result = enrich(service(request -> success(), gateway), emptyIndex());

        assertFalse(result.cacheHit());
        assertEquals(ResultDisposition.MODEL_SUCCESS, result.envelope().resultDisposition());
        assertEquals("tool-test", gateway.runReference);
        assertEquals(result.envelope().outputHash(), gateway.result.outputHash());
        assertTrue(gateway.result.artifactUri().startsWith("repo://renovatio-llm/"));
        assertTrue(Files.isRegularFile(cache().pathFor(result.envelope().cacheKey())));
    }

    @Test
    void providerFailureCreatesAttributedDeterministicFallback() {
        CapturingGateway gateway = new CapturingGateway(false);
        EnrichmentResult result = enrich(service(request -> {
            throw new ProviderException(ProviderFailure.PROVIDER_TIMEOUT);
        }, gateway), emptyIndex());

        assertEquals(ResultDisposition.DETERMINISTIC_FALLBACK, result.envelope().resultDisposition());
        assertEquals("PROVIDER_TIMEOUT", result.envelope().failureCategory());
        assertEquals("PROVIDER_TIMEOUT", gateway.result.failureCategory());
    }

    @Test
    void schemaFailureUsesCatalogFallbackAndNeverPersistsModelProposal() {
        CapturingGateway gateway = new CapturingGateway(false);
        EnrichmentResult result = enrich(service(request -> new LlmResponse("anthropic", "model",
                JSON.createObjectNode().put("suggestedName", "missing-rationale")), gateway), emptyIndex());

        assertEquals(ResultDisposition.DETERMINISTIC_FALLBACK, result.envelope().resultDisposition());
        assertEquals("OUTPUT_SCHEMA_INVALID", result.envelope().failureCategory());
        assertEquals("LLM_MANUAL_DOMAIN_NAMING_REQUIRED",
                result.envelope().sanitizedResult().path("diagnosticCode").textValue());
        assertFalse(result.envelope().sanitizedResult().toString().contains("missing-rationale"));
    }

    @Test
    void annotatedIrSemanticMismatchUsesDeterministicFallback() {
        String nodeId = "a".repeat(64);
        JsonNode mismatchedInput = JSON.createObjectNode()
                .put("nodeId", nodeId)
                .put("nodeKind", "PARAGRAPH")
                .set("semanticNodes", JSON.createObjectNode().put(nodeId, "DATA_ITEM"));
        CommittedCacheIndex index = emptyIndex();

        EnrichmentResult result = service(request -> success(), new CapturingGateway(false)).enrich(
                "cobol.domain.naming.v1", mismatchedInput, "anthropic", "model",
                JSON.createObjectNode().put("rationale", "deterministic translation"), index,
                new VerifiedPromotionManifest(VerifiedPromotionManifest.VERSION, index.digest(), Map.of()));

        assertEquals(ResultDisposition.DETERMINISTIC_FALLBACK, result.envelope().resultDisposition());
        assertEquals("VALIDATOR_REJECTED", result.envelope().failureCategory());
    }

    @Test
    void providerRequestIsBoundToCatalogSystemFewShotAndInput() {
        java.util.concurrent.atomic.AtomicReference<org.shark.renovatio.llm.provider.LlmRequest> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        enrich(service(request -> { captured.set(request); return success(); },
                new CapturingGateway(false)), emptyIndex());

        assertEquals("cobol.domain.naming.v1", captured.get().promptId());
        assertEquals("Suggest a concise Java domain name without changing COBOL semantics.",
                captured.get().systemPrompt());
        assertEquals(1, captured.get().fewShot().size());
        assertEquals(input(), captured.get().input());
    }

    @Test
    void verifiedCommittedHitMakesZeroProviderAndAttributionCalls() throws Exception {
        String key = preparedKey();
        org.shark.renovatio.llm.cache.CacheIdentity identity = runtime().prepare(
                "cobol.domain.naming.v1", input(), "anthropic", "model").identity();
        CacheEnvelope pending = CacheEnvelope.pending(key, ResultDisposition.DETERMINISTIC_FALLBACK,
                identity, CacheKey.sha256("input"), CacheKey.sha256("output"), "PROVIDER_TIMEOUT",
                JSON.createObjectNode().put("diagnosticCode", "LLM_PROVIDER_TIMEOUT")
                        .put("manualAction", "Review manually")
                        .set("deterministicResult", JSON.createObjectNode().put("rationale", "deterministic")),
                "tool-test", "repo://renovatio-llm/src/main/resources/llm-cache/"
                        + key.substring(0, 2) + "/" + key + ".json");
        Path path = cache().writeCandidate(pending);
        CacheEnvelope committed = pending.promote();
        JSON.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), committed);
        String relative = cacheRoot().relativize(path).toString();
        String contentHash = CacheKey.sha256(Files.readAllBytes(path));
        CommittedCacheIndex index = new CommittedCacheIndex(CommittedCacheIndex.VERSION, Map.of(key,
                new CommittedCacheIndex.Entry(relative, committed.envelopeHash(), contentHash)));
        VerifiedPromotionManifest manifest = new VerifiedPromotionManifest(
                VerifiedPromotionManifest.VERSION, index.digest(), Map.of(key,
                new VerifiedPromotionManifest.Entry(relative, committed.envelopeHash(), contentHash,
                        "a".repeat(40), "b".repeat(40), "c".repeat(40),
                        "repo://.agora/evidence/cache-promotion.md")));
        AtomicInteger providerCalls = new AtomicInteger();
        AtomicInteger attributionCalls = new AtomicInteger();
        AttributionGateway gateway = new AttributionGateway() {
            @Override public String begin(AttributionInput input) { attributionCalls.incrementAndGet(); return "x"; }
            @Override public void complete(String runReference, AttributionResult result) {
                attributionCalls.incrementAndGet();
            }
        };
        GovernedEnrichmentService service = new GovernedEnrichmentService(() -> {
            providerCalls.incrementAndGet();
            throw new IllegalStateException("provider configuration must remain lazy on a hit");
        }, cache(), gateway, new PersistenceSanitizer(), runtime());

        EnrichmentResult result = service.enrich("cobol.domain.naming.v1", input(), "anthropic", "model",
                JSON.createObjectNode().put("rationale", "deterministic translation"), index, manifest);

        assertTrue(result.cacheHit());
        assertEquals(0, providerCalls.get());
        assertEquals(0, attributionCalls.get());
    }

    @Test
    void finalizationFailureQuarantinesAndRemovesCandidate() throws Exception {
        AttributionException exception = assertThrows(AttributionException.class,
                () -> enrich(service(request -> success(), new CapturingGateway(true)), emptyIndex()));

        assertEquals(AttributionException.Stage.FINALIZE, exception.stage());
        String key = preparedKey();
        assertFalse(Files.exists(cache().pathFor(key)));
        assertTrue(Files.isRegularFile(temporary.resolve("quarantine").resolve(key + ".invalid.json")));
    }

    @Test
    void sanitizerRejectsUnknownFieldsAndSecretLikeContent() {
        PersistenceSanitizer sanitizer = new PersistenceSanitizer();
        assertThrows(PersistenceSanitizer.SanitizationException.class,
                () -> sanitizer.sanitize(JSON.createObjectNode().put("rawOutput", "value")));
        assertThrows(PersistenceSanitizer.SanitizationException.class,
                () -> sanitizer.sanitize(JSON.createObjectNode().put("rationale", "Bearer secret-token")));
    }

    @Test
    void fallbackAcceptsDomainFieldsInsideDeterministicResult() {
        EnrichmentResult result = service(request -> {
            throw new ProviderException(ProviderFailure.PROVIDER_TIMEOUT);
        }, new CapturingGateway(false)).enrich("cobol.domain.naming.v1", input(), "anthropic", "model",
                JSON.createObjectNode().put("legacyCode", "A1")
                        .set("nested", JSON.createObjectNode().put("calculatedAmount", 42)),
                emptyIndex(), emptyManifest());

        JsonNode deterministic = result.envelope().sanitizedResult().path("deterministicResult");
        assertEquals("A1", deterministic.path("legacyCode").textValue());
        assertEquals(42, deterministic.path("nested").path("calculatedAmount").intValue());
        assertEquals(ResultDisposition.DETERMINISTIC_FALLBACK, result.envelope().resultDisposition());
    }

    @Test
    void deterministicResultStillRejectsSecretsAndForbiddenFields() {
        PersistenceSanitizer sanitizer = new PersistenceSanitizer();
        assertThrows(PersistenceSanitizer.SanitizationException.class, () -> sanitizer.sanitize(
                JSON.createObjectNode().put("diagnosticCode", "LLM_FALLBACK")
                        .set("deterministicResult", JSON.createObjectNode().put("apiKey", "secret"))));
        assertThrows(PersistenceSanitizer.SanitizationException.class, () -> sanitizer.sanitize(
                JSON.createObjectNode().put("diagnosticCode", "LLM_FALLBACK")
                        .set("deterministicResult", JSON.createObjectNode()
                                .put("domainValue", "Bearer secret-token"))));
    }

    @Test
    void productionGatewayRequiresMatchingRunningAgoraToolRun() throws Exception {
        Path run = temporary.resolve("tool-abc123").resolve("RUN.md");
        Files.createDirectories(run.getParent());
        Files.writeString(run, """
                ---
                schema: "agora/tool-run/v1"
                id: "tool-abc123"
                tool: "llm-enrichment"
                operation: "enrich"
                status: "running"
                inputs: {"prompt-id":"cobol.domain.naming.v1","provider":"anthropic","model":"model","input-hash":"%s","cache-key":"%s","schema-hash":"%s","runtime-contract-version":"renovatio-llm.v1"}
                ---
                # Tool run
                """.formatted("a".repeat(64), "b".repeat(64), "c".repeat(64)));
        java.util.concurrent.atomic.AtomicReference<AttributionResult> result =
                new java.util.concurrent.atomic.AtomicReference<>();
        AgoraToolRunAttributionGateway gateway = new AgoraToolRunAttributionGateway(
                Map.of(AgoraToolRunAttributionGateway.TOOL_RUN_ENV, run.toString()), result::set);
        AttributionInput input = new AttributionInput("cobol.domain.naming.v1", "anthropic", "model",
                "a".repeat(64), "b".repeat(64), "c".repeat(64), "renovatio-llm.v1");

        assertEquals("tool-abc123", gateway.begin(input));
        assertThrows(AttributionException.class, () -> gateway.begin(new AttributionInput(
                "cobol.domain.naming.v1", "anthropic", "other", "a".repeat(64),
                "b".repeat(64), "c".repeat(64), "renovatio-llm.v1")));
    }

    private GovernedEnrichmentService service(LlmProvider provider, AttributionGateway gateway) {
        PromptRuntime runtime = runtime();
        return new GovernedEnrichmentService(provider, cache(), gateway, new PersistenceSanitizer(), runtime);
    }

    private ContentAddressedCache cache() {
        return new ContentAddressedCache(cacheRoot(), temporary.resolve("quarantine"));
    }

    private Path cacheRoot() {
        return temporary.resolve("cache");
    }

    private static LlmResponse success() {
        return new LlmResponse("anthropic", "model",
                JSON.createObjectNode().put("suggestedName", "calculateInterest")
                        .put("rationale", "Describes the paragraph action"));
    }

    private static EnrichmentResult enrich(GovernedEnrichmentService service, CommittedCacheIndex index) {
        return service.enrich("cobol.domain.naming.v1", input(), "anthropic", "model",
                JSON.createObjectNode().put("rationale", "deterministic translation"), index,
                new VerifiedPromotionManifest(VerifiedPromotionManifest.VERSION, index.digest(), Map.of()));
    }

    private static JsonNode input() {
        return JSON.createObjectNode().put("nodeId", "node-1");
    }

    private static PromptRuntime runtime() {
        return new PromptRuntime(new PromptCatalogLoader().loadDefault());
    }

    private static String preparedKey() {
        return CacheKey.derive(runtime().prepare("cobol.domain.naming.v1", input(),
                "anthropic", "model").identity());
    }

    private static CommittedCacheIndex emptyIndex() {
        return new CommittedCacheIndex(CommittedCacheIndex.VERSION, Map.of());
    }

    private static VerifiedPromotionManifest emptyManifest() {
        CommittedCacheIndex index = emptyIndex();
        return new VerifiedPromotionManifest(VerifiedPromotionManifest.VERSION, index.digest(), Map.of());
    }

    private static final class CapturingGateway implements AttributionGateway {
        private final boolean failCompletion;
        private String runReference;
        private AttributionResult result;

        private CapturingGateway(boolean failCompletion) {
            this.failCompletion = failCompletion;
        }

        @Override
        public String begin(AttributionInput input) {
            runReference = "tool-test";
            return runReference;
        }

        @Override
        public void complete(String runReference, AttributionResult result) {
            if (failCompletion) throw new IllegalStateException();
            this.result = result;
        }
    }
}
