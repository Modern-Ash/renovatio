package org.shark.renovatio.llm.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.DecisionPoint.LlmFailureCategory;
import org.shark.renovatio.decisions.DecisionTransitions;
import org.shark.renovatio.llm.cache.CommittedCacheIndex;
import org.shark.renovatio.llm.cache.ResultDisposition;
import org.shark.renovatio.llm.cache.VerifiedPromotionManifest;
import org.shark.renovatio.llm.enrichment.AttributionException;
import org.shark.renovatio.llm.enrichment.EnrichmentResult;
import org.shark.renovatio.llm.enrichment.GovernedEnrichmentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Bounded, option-only suggestions. All failures retain deterministic defaults. */
public final class DecisionSuggestionService {
    public static final BigDecimal ELIGIBILITY_THRESHOLD = new BigDecimal("0.8");
    private static final ObjectMapper JSON = new ObjectMapper();
    private final SuggestionRuntime runtime;

    public DecisionSuggestionService(SuggestionRuntime runtime) {
        this.runtime = java.util.Objects.requireNonNull(runtime);
    }

    public SuggestionBatch suggest(List<DecisionPoint> current, String profileHash,
                                   int providerCallCap, Instant now) {
        if (providerCallCap < 0 || providerCallCap > 100) throw new IllegalArgumentException("providerCallCap");
        List<DecisionPoint> result = new ArrayList<>(current);
        List<DecisionPoint> eligible = current.stream().filter(DecisionPoint::active)
                .filter(value -> value.status() == DecisionPoint.Status.AUTO)
                .filter(value -> value.confidence().compareTo(ELIGIBILITY_THRESHOLD) < 0)
                .sorted(Comparator.comparing(DecisionPoint::confidence).thenComparing(DecisionPoint::id)).toList();
        int attempted = 0;
        int failed = 0;
        int cacheHits = 0;
        int providerCalls = 0;
        for (DecisionPoint decision : eligible) {
            RuntimeResult evaluated;
            try {
                String promptId = promptId(decision.category());
                ObjectNode canonicalInput = input(decision, profileHash);
                Optional<RuntimeResult> cached = runtime.lookup(promptId, canonicalInput, deterministic(decision));
                if (cached.isPresent()) evaluated = cached.get();
                else {
                    if (providerCalls >= providerCallCap) continue;
                    evaluated = runtime.evaluate(promptId, canonicalInput, deterministic(decision));
                }
            } catch (AttributionException exception) {
                evaluated = RuntimeResult.failure(LlmFailureCategory.ATTRIBUTION_ERROR, false);
            } catch (RuntimeException exception) {
                evaluated = RuntimeResult.failure(LlmFailureCategory.CACHE_ERROR, false);
            }
            if (evaluated.cacheHit()) cacheHits++;
            else { attempted++; providerCalls++; }

            DecisionPoint next;
            if (evaluated.failure() != null) {
                failed++;
                next = DecisionTransitions.llmFailure(decision, evaluated.failure(), now);
            } else {
                Validation validation = validate(evaluated.output(), decision);
                if (validation.failure() != null) {
                    failed++;
                    next = DecisionTransitions.llmFailure(decision, validation.failure(), now);
                } else {
                    next = DecisionTransitions.suggest(decision, validation.option(), validation.confidence(),
                            validation.rationale(), now);
                }
            }
            result.set(result.indexOf(decision), next);
        }
        return new SuggestionBatch(List.copyOf(result), attempted, failed, cacheHits);
    }

    private static ObjectNode input(DecisionPoint decision, String profileHash) {
        ObjectNode input = JSON.createObjectNode();
        input.put("semanticIrHash", decision.semanticIrHash());
        input.put("profileHash", profileHash);
        input.put("decisionId", decision.id());
        input.put("decisionKey", decision.decisionKey());
        input.put("category", decision.category().name());
        input.set("location", JSON.valueToTree(decision.location()));
        input.set("options", JSON.valueToTree(decision.options()));
        input.put("defaultOption", decision.defaultOption());
        input.set("evidence", JSON.valueToTree(decision.evidence()));
        return input;
    }

    private static ObjectNode deterministic(DecisionPoint decision) {
        ObjectNode result = JSON.createObjectNode();
        result.put("chosenOption", decision.defaultOption());
        result.put("confidence", decision.confidence());
        result.put("rationale", decision.rationale());
        return result;
    }

    private static Validation validate(JsonNode output, DecisionPoint decision) {
        if (output == null || !output.isObject()) return Validation.failure(LlmFailureCategory.MALFORMED_JSON);
        if (output.size() != 3 || !output.has("chosenOption") || !output.has("confidence") || !output.has("rationale"))
            return Validation.failure(LlmFailureCategory.SCHEMA_INVALID);
        String option = output.path("chosenOption").asText(null);
        if (option == null || !decision.options().contains(option))
            return Validation.failure(LlmFailureCategory.OPTION_INVALID);
        if (!output.path("confidence").isNumber()) return Validation.failure(LlmFailureCategory.SCHEMA_INVALID);
        BigDecimal confidence = output.path("confidence").decimalValue();
        if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0)
            return Validation.failure(LlmFailureCategory.SCHEMA_INVALID);
        String rationale = output.path("rationale").asText(null);
        if (rationale == null || rationale.isBlank() || rationale.length() > 4_000 || containsSecret(rationale))
            return Validation.failure(LlmFailureCategory.SANITIZATION_FAILED);
        return new Validation(option, confidence, rationale.trim(), null);
    }

    private static boolean containsSecret(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("bearer ") || normalized.contains("api_key") || normalized.contains("sk-");
    }

    public static String promptId(DecisionPoint.Category category) {
        return "decision." + category.name().toLowerCase(Locale.ROOT).replace('_', '-') + ".v1";
    }

    private static LlmFailureCategory mapFailure(String failure) {
        if (failure == null) return LlmFailureCategory.PROVIDER_ERROR;
        return switch (failure) {
            case "PROVIDER_TIMEOUT" -> LlmFailureCategory.TIMEOUT;
            case "OUTPUT_MALFORMED" -> LlmFailureCategory.MALFORMED_JSON;
            case "OUTPUT_SCHEMA_INVALID" -> LlmFailureCategory.SCHEMA_INVALID;
            case "VALIDATOR_REJECTED" -> LlmFailureCategory.OPTION_INVALID;
            case "SANITIZATION_REJECTED" -> LlmFailureCategory.SANITIZATION_FAILED;
            default -> LlmFailureCategory.PROVIDER_ERROR;
        };
    }

    public static SuggestionRuntime governed(GovernedEnrichmentService service,
                                             String provider, String model,
                                             CommittedCacheIndex index,
                                             VerifiedPromotionManifest manifest) {
        return new SuggestionRuntime() {
            @Override public Optional<RuntimeResult> lookup(String promptId, JsonNode input, JsonNode deterministic) {
                return service.findCommitted(promptId, input, provider, model, index, manifest)
                        .map(DecisionSuggestionService::runtimeResult);
            }
            @Override public RuntimeResult evaluate(String promptId, JsonNode input, JsonNode deterministic) {
                return runtimeResult(service.enrich(promptId, input, provider, model, deterministic, index, manifest));
            }
        };
    }

    private static RuntimeResult runtimeResult(EnrichmentResult result) {
        if (result.envelope().resultDisposition() == ResultDisposition.DETERMINISTIC_FALLBACK)
            return RuntimeResult.failure(mapFailure(result.envelope().failureCategory()), result.cacheHit());
        return new RuntimeResult(result.envelope().sanitizedResult(), result.cacheHit(), null);
    }

    @FunctionalInterface
    public interface SuggestionRuntime {
        RuntimeResult evaluate(String promptId, JsonNode canonicalInput, JsonNode deterministicResult);
        default Optional<RuntimeResult> lookup(String promptId, JsonNode canonicalInput,
                                               JsonNode deterministicResult) {
            return Optional.empty();
        }
    }

    public record RuntimeResult(JsonNode output, boolean cacheHit, LlmFailureCategory failure) {
        public static RuntimeResult success(JsonNode output, boolean cacheHit) {
            return new RuntimeResult(output, cacheHit, null);
        }
        public static RuntimeResult failure(LlmFailureCategory failure, boolean cacheHit) {
            return new RuntimeResult(null, cacheHit, failure);
        }
    }
    public record SuggestionBatch(List<DecisionPoint> decisions, int suggestionsAttempted,
                                  int suggestionsFailed, int cacheHits) { }
    private record Validation(String option, BigDecimal confidence, String rationale,
                              LlmFailureCategory failure) {
        static Validation failure(LlmFailureCategory failure) {
            return new Validation(null, null, null, failure);
        }
    }
}
