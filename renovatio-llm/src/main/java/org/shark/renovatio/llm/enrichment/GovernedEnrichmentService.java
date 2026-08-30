package org.shark.renovatio.llm.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;
import org.shark.renovatio.llm.cache.CacheEnvelope;
import org.shark.renovatio.llm.cache.CacheIdentity;
import org.shark.renovatio.llm.cache.CacheKey;
import org.shark.renovatio.llm.cache.CommittedCacheIndex;
import org.shark.renovatio.llm.cache.ContentAddressedCache;
import org.shark.renovatio.llm.cache.ResultDisposition;
import org.shark.renovatio.llm.cache.VerifiedPromotionManifest;
import org.shark.renovatio.llm.provider.LlmProvider;
import org.shark.renovatio.llm.provider.LlmResponse;
import org.shark.renovatio.llm.provider.ProviderException;
import org.shark.renovatio.llm.prompt.CatalogFallbackFactory;
import org.shark.renovatio.llm.prompt.OutputValidationException;
import org.shark.renovatio.llm.prompt.PreparedEnrichment;
import org.shark.renovatio.llm.prompt.PromptOutputValidator;
import org.shark.renovatio.llm.prompt.PromptRuntime;

import java.util.Objects;
import java.util.function.Supplier;

/** Cache-first enrichment whose miss path is fully enclosed by Agora attribution. */
public final class GovernedEnrichmentService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Supplier<LlmProvider> provider;
    private final ContentAddressedCache cache;
    private final AttributionGateway attribution;
    private final PersistenceSanitizer sanitizer;
    private final PromptRuntime promptRuntime;
    private final PromptOutputValidator outputValidator;
    private final CatalogFallbackFactory fallbackFactory;

    public GovernedEnrichmentService(LlmProvider provider, ContentAddressedCache cache,
                                     AttributionGateway attribution, PersistenceSanitizer sanitizer,
                                     PromptRuntime promptRuntime) {
        this(() -> Objects.requireNonNull(provider), cache, attribution, sanitizer, promptRuntime);
    }

    public GovernedEnrichmentService(Supplier<LlmProvider> provider, ContentAddressedCache cache,
                                     AttributionGateway attribution, PersistenceSanitizer sanitizer,
                                     PromptRuntime promptRuntime) {
        this.provider = Objects.requireNonNull(provider);
        this.cache = Objects.requireNonNull(cache);
        this.attribution = Objects.requireNonNull(attribution);
        this.sanitizer = Objects.requireNonNull(sanitizer);
        this.promptRuntime = Objects.requireNonNull(promptRuntime);
        this.outputValidator = new PromptOutputValidator(promptRuntime, sanitizer);
        this.fallbackFactory = new CatalogFallbackFactory(promptRuntime);
    }

    public EnrichmentResult enrich(String promptId, JsonNode canonicalInput, String providerId,
                                   String model, JsonNode deterministicResult,
                                   CommittedCacheIndex index, VerifiedPromotionManifest manifest) {
        PreparedEnrichment prepared = promptRuntime.prepare(promptId, canonicalInput, providerId, model);
        CacheIdentity identity = prepared.identity();
        String cacheKey = CacheKey.derive(identity);
        return cache.find(cacheKey, index, manifest)
                .map(envelope -> new EnrichmentResult(envelope, true))
                .orElseGet(() -> miss(prepared, deterministicResult, cacheKey));
    }

    private EnrichmentResult miss(PreparedEnrichment prepared, JsonNode deterministicResult, String cacheKey) {
        CacheIdentity identity = prepared.identity();
        String inputHash = hash(identity.canonicalInput());
        // Provider construction is configuration preflight. It must happen after the cache lookup
        // but before Agora attribution so invalid credentials/model cannot create a miss record.
        LlmProvider resolvedProvider = Objects.requireNonNull(provider.get());
        String runReference;
        try {
            runReference = attribution.begin(new AttributionInput(identity.promptId(), identity.provider(),
                    identity.model(), inputHash, cacheKey, identity.outputSchemaHash(),
                    CacheIdentity.RUNTIME_CONTRACT_VERSION));
            if (runReference == null || runReference.isBlank()) throw new RuntimeException();
        } catch (RuntimeException exception) {
            throw new AttributionException(AttributionException.Stage.INIT);
        }

        ResultDisposition disposition;
        String failureCategory = null;
        JsonNode sanitized;
        try {
            LlmResponse response = resolvedProvider.complete(prepared.request());
            sanitized = outputValidator.validate(prepared, response.content());
            disposition = ResultDisposition.MODEL_SUCCESS;
        } catch (ProviderException exception) {
            failureCategory = exception.failure().name();
            sanitized = sanitizer.sanitize(fallbackFactory.create(prepared.definition(),
                    exception.failure(), deterministicResult));
            disposition = ResultDisposition.DETERMINISTIC_FALLBACK;
        } catch (OutputValidationException exception) {
            failureCategory = exception.failure().name();
            sanitized = sanitizer.sanitize(fallbackFactory.create(prepared.definition(),
                    exception.failure(), deterministicResult));
            disposition = ResultDisposition.DETERMINISTIC_FALLBACK;
        } catch (PersistenceSanitizer.SanitizationException exception) {
            failureCategory = "SANITIZATION_REJECTED";
            sanitized = sanitizer.sanitize(fallbackFactory.create(prepared.definition(),
                    org.shark.renovatio.llm.provider.ProviderFailure.SANITIZATION_REJECTED,
                    deterministicResult));
            disposition = ResultDisposition.DETERMINISTIC_FALLBACK;
        }

        String outputHash = hash(sanitized);
        String artifactUri = "repo://renovatio-llm/src/main/resources/llm-cache/"
                + cacheKey.substring(0, 2) + "/" + cacheKey + ".json";
        CacheEnvelope envelope = CacheEnvelope.pending(cacheKey, disposition, identity, inputHash,
                outputHash, failureCategory, sanitized, runReference, artifactUri);
        cache.writeCandidate(envelope);
        try {
            attribution.complete(runReference, new AttributionResult(outputHash, disposition.name(),
                    envelope.promotionDisposition().name(), failureCategory, artifactUri,
                    identity.outputSchemaHash(), cacheKey, envelope.envelopeHash(),
                    CacheIdentity.RUNTIME_CONTRACT_VERSION));
        } catch (RuntimeException exception) {
            cache.quarantine(envelope);
            throw new AttributionException(AttributionException.Stage.FINALIZE);
        }
        return new EnrichmentResult(envelope, false);
    }

    private static String hash(JsonNode value) {
        Object projection = JSON.convertValue(value, Object.class);
        return CacheKey.sha256(CanonicalJson.write(projection));
    }
}
