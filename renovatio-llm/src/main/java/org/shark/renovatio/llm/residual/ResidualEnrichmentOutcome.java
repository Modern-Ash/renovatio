package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.JsonNode;

/** Routing outcome; deterministic results and residual proposals are never conflated. */
public record ResidualEnrichmentOutcome(
        ResidualRoute route,
        JsonNode deterministicResult,
        JsonNode proposal,
        String diagnosticCode,
        String manualAction) {

    static ResidualEnrichmentOutcome deterministic(JsonNode deterministicResult) {
        return new ResidualEnrichmentOutcome(ResidualRoute.DETERMINISTIC, deterministicResult,
                null, null, null);
    }

    static ResidualEnrichmentOutcome enriched(ResidualRoute route, JsonNode deterministicResult,
                                               JsonNode proposal) {
        return new ResidualEnrichmentOutcome(route, deterministicResult, proposal, null, null);
    }
}
