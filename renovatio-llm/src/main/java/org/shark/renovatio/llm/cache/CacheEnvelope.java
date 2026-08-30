package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;

/** Strict v1 cache artifact. Its own hash is excluded from hash calculation. */
public record CacheEnvelope(
        String envelopeVersion,
        String cacheKey,
        ResultDisposition resultDisposition,
        PromotionDisposition promotionDisposition,
        String promptId,
        String provider,
        String model,
        String outputSchemaId,
        String outputSchemaHash,
        List<String> validators,
        String runtimeContractVersion,
        String inputHash,
        String outputHash,
        String governedRunReference,
        String artifactUri,
        String failureCategory,
        JsonNode sanitizedResult,
        String envelopeHash) {

    private static final ObjectMapper JSON = new ObjectMapper();
    public static final String VERSION = "renovatio-llm-cache.v1";

    public CacheEnvelope {
        Objects.requireNonNull(envelopeVersion);
        Objects.requireNonNull(resultDisposition);
        Objects.requireNonNull(promotionDisposition);
        Objects.requireNonNull(sanitizedResult);
        validators = List.copyOf(Objects.requireNonNull(validators));
        if (validators.isEmpty()) throw new IllegalArgumentException("validators");
        requireText(outputSchemaId, "outputSchemaId");
        requireHash(outputSchemaHash, "outputSchemaHash");
        requireText(runtimeContractVersion, "runtimeContractVersion");
        requireText(governedRunReference, "governedRunReference");
        requireText(artifactUri, "artifactUri");
        requireHash(cacheKey, "cacheKey");
        requireHash(inputHash, "inputHash");
        requireHash(outputHash, "outputHash");
        requireHash(envelopeHash, "envelopeHash");
        if (resultDisposition == ResultDisposition.DETERMINISTIC_FALLBACK
                && (failureCategory == null || failureCategory.isBlank())) {
            throw new IllegalArgumentException("Fallback requires failureCategory");
        }
    }

    public static CacheEnvelope pending(String cacheKey, ResultDisposition resultDisposition,
                                        CacheIdentity identity,
                                        String inputHash, String outputHash, String failureCategory,
                                        JsonNode sanitizedResult, String governedRunReference,
                                        String artifactUri) {
        CacheEnvelope unhashed = new CacheEnvelope(VERSION, cacheKey, resultDisposition,
                PromotionDisposition.PENDING_PROMOTION, identity.promptId(), identity.provider(),
                identity.model(), identity.outputSchemaId(), identity.outputSchemaHash(),
                identity.validators(), CacheIdentity.RUNTIME_CONTRACT_VERSION, inputHash,
                outputHash, governedRunReference, artifactUri, failureCategory, sanitizedResult,
                "0".repeat(64));
        return unhashed.withHash(unhashed.calculateHash());
    }

    public CacheEnvelope promote() {
        CacheEnvelope promoted = new CacheEnvelope(envelopeVersion, cacheKey, resultDisposition,
                PromotionDisposition.COMMITTED, promptId, provider, model, outputSchemaId,
                outputSchemaHash, validators, runtimeContractVersion, inputHash, outputHash,
                governedRunReference, artifactUri, failureCategory, sanitizedResult, "0".repeat(64));
        return promoted.withHash(promoted.calculateHash());
    }

    public CacheEnvelope invalidateAttribution() {
        CacheEnvelope invalid = new CacheEnvelope(envelopeVersion, cacheKey, resultDisposition,
                PromotionDisposition.INVALID_ATTRIBUTION, promptId, provider, model, outputSchemaId,
                outputSchemaHash, validators, runtimeContractVersion, inputHash, outputHash,
                governedRunReference, artifactUri, failureCategory, sanitizedResult, "0".repeat(64));
        return invalid.withHash(invalid.calculateHash());
    }

    public boolean hasValidHash() {
        return envelopeHash.equals(calculateHash());
    }

    private CacheEnvelope withHash(String hash) {
        return new CacheEnvelope(envelopeVersion, cacheKey, resultDisposition, promotionDisposition,
                promptId, provider, model, outputSchemaId, outputSchemaHash, validators,
                runtimeContractVersion, inputHash, outputHash, governedRunReference, artifactUri,
                failureCategory, sanitizedResult, hash);
    }

    private String calculateHash() {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("envelopeVersion", envelopeVersion);
        projection.put("cacheKey", cacheKey);
        projection.put("resultDisposition", resultDisposition.name());
        projection.put("promotionDisposition", promotionDisposition.name());
        projection.put("promptId", promptId);
        projection.put("provider", provider);
        projection.put("model", model);
        projection.put("outputSchemaId", outputSchemaId);
        projection.put("outputSchemaHash", outputSchemaHash);
        projection.put("validators", validators);
        projection.put("runtimeContractVersion", runtimeContractVersion);
        projection.put("inputHash", inputHash);
        projection.put("outputHash", outputHash);
        projection.put("governedRunReference", governedRunReference);
        projection.put("artifactUri", artifactUri);
        if (failureCategory != null) projection.put("failureCategory", failureCategory);
        projection.put("sanitizedResult", JSON.convertValue(sanitizedResult, Object.class));
        return CacheKey.sha256(CanonicalJson.write(projection));
    }

    private static void requireHash(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field);
    }
}
