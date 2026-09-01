package org.shark.renovatio.llm.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.F1DecisionCatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DecisionSuggestionServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void realF1CatalogMakesZeroRuntimeCalls() {
        AtomicInteger calls = new AtomicInteger();
        DecisionSuggestionService service = new DecisionSuggestionService((prompt, input, fallback) -> {
            calls.incrementAndGet();
            return DecisionSuggestionService.RuntimeResult.success(valid("CURRENT_PIC_MAPPING"), false);
        });
        var batch = service.suggest(F1DecisionCatalog.create("a".repeat(64), NOW), "b".repeat(64), 100, NOW);
        assertEquals(0, calls.get());
        assertEquals(0, batch.suggestionsAttempted());
        assertEquals(7, batch.decisions().size());
    }

    @Test
    void eligibleSyntheticDecisionBecomesSuggested() {
        DecisionPoint decision = lowConfidence();
        DecisionSuggestionService service = new DecisionSuggestionService((prompt, input, fallback) -> {
            assertEquals("decision.naming.v1", prompt);
            assertEquals(decision.id(), input.path("decisionId").asText());
            return DecisionSuggestionService.RuntimeResult.success(valid("FLUENT"), false);
        });
        var batch = service.suggest(List.of(decision), "b".repeat(64), 1, NOW.plusSeconds(1));
        DecisionPoint suggested = batch.decisions().get(0);
        assertEquals(DecisionPoint.Status.SUGGESTED, suggested.status());
        assertEquals(DecisionPoint.Source.LLM, suggested.source());
        assertEquals("FLUENT", suggested.chosenOption());
        assertEquals(1, batch.suggestionsAttempted());
    }

    @Test
    void invalidOptionAndRuntimeFailureKeepDeterministicDefault() {
        DecisionPoint decision = lowConfidence();
        DecisionSuggestionService invalid = new DecisionSuggestionService((prompt, input, fallback) ->
                DecisionSuggestionService.RuntimeResult.success(valid("INVENTED"), true));
        var first = invalid.suggest(List.of(decision), "b".repeat(64), 1, NOW).decisions().get(0);
        assertEquals(DecisionPoint.Status.AUTO, first.status());
        assertEquals(DecisionPoint.LlmFailureCategory.OPTION_INVALID, first.llmFailureCategory());
        assertEquals("JAVA_BEANS", first.chosenOption());

        DecisionSuggestionService timeout = new DecisionSuggestionService((prompt, input, fallback) ->
                DecisionSuggestionService.RuntimeResult.failure(DecisionPoint.LlmFailureCategory.TIMEOUT, false));
        var second = timeout.suggest(List.of(decision), "b".repeat(64), 1, NOW).decisions().get(0);
        assertEquals(DecisionPoint.LlmFailureCategory.TIMEOUT, second.llmFailureCategory());
    }

    @Test
    void cacheHitDoesNotConsumeProviderAttemptCount() {
        DecisionSuggestionService service = new DecisionSuggestionService((prompt, input, fallback) ->
                DecisionSuggestionService.RuntimeResult.success(valid("JAVA_BEANS"), true));
        var batch = service.suggest(List.of(lowConfidence()), "b".repeat(64), 1, NOW);
        assertEquals(0, batch.suggestionsAttempted());
        assertEquals(1, batch.cacheHits());
    }

    @Test
    void committedCacheHitsStillResolveAfterProviderCapIsReached() {
        DecisionPoint first = lowConfidence();
        DecisionPoint second = new DecisionPoint(first.schemaVersion(), "f".repeat(64), first.category(),
                "java.naming.second", first.location(), first.question(), first.options(), first.defaultOption(),
                first.chosenOption(), first.source(), new BigDecimal("0.6"), first.rationale(), first.evidence(),
                first.status(), first.semanticIrHash(), false, null, 1, NOW, NOW, true);
        AtomicInteger providerCalls = new AtomicInteger();
        DecisionSuggestionService.SuggestionRuntime runtime = new DecisionSuggestionService.SuggestionRuntime() {
            @Override public DecisionSuggestionService.RuntimeResult evaluate(String prompt, com.fasterxml.jackson.databind.JsonNode input,
                                                                               com.fasterxml.jackson.databind.JsonNode fallback) {
                providerCalls.incrementAndGet();
                return DecisionSuggestionService.RuntimeResult.success(valid("FLUENT"), false);
            }
            @Override public Optional<DecisionSuggestionService.RuntimeResult> lookup(String prompt,
                    com.fasterxml.jackson.databind.JsonNode input, com.fasterxml.jackson.databind.JsonNode fallback) {
                return input.path("decisionId").asText().equals(second.id())
                        ? Optional.of(DecisionSuggestionService.RuntimeResult.success(valid("JAVA_BEANS"), true))
                        : Optional.empty();
            }
        };

        var batch = new DecisionSuggestionService(runtime).suggest(List.of(first, second), "b".repeat(64), 1, NOW);

        assertEquals(1, providerCalls.get());
        assertEquals(1, batch.suggestionsAttempted());
        assertEquals(1, batch.cacheHits());
        assertTrue(batch.decisions().stream().allMatch(value -> value.status() == DecisionPoint.Status.SUGGESTED));
    }

    private static DecisionPoint lowConfidence() {
        DecisionPoint value = F1DecisionCatalog.create("a".repeat(64), NOW).get(3);
        return new DecisionPoint(value.schemaVersion(), value.id(), value.category(), value.decisionKey(),
                value.location(), value.question(), value.options(), value.defaultOption(), value.chosenOption(),
                value.source(), new BigDecimal("0.5"), value.rationale(), value.evidence(), value.status(),
                value.semanticIrHash(), false, null, value.revision(), value.createdAt(), value.updatedAt(), true);
    }

    private static ObjectNode valid(String option) {
        ObjectNode output = JSON.createObjectNode();
        output.put("chosenOption", option);
        output.put("confidence", 0.75);
        output.put("rationale", "Bounded recommendation.");
        return output;
    }
}
