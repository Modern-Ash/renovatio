package org.shark.renovatio.llm.eval;

import java.util.List;

/** Deterministic evaluation summary for governed LLM enrichment batches. */
public record LlmEvaluation(String datasetId, int total, int accepted, int schemaFailures,
                            int provenanceFailures, List<String> failures) {
    public LlmEvaluation {
        if (datasetId == null || datasetId.isBlank() || total < 0 || accepted < 0 || schemaFailures < 0 || provenanceFailures < 0)
            throw new IllegalArgumentException("invalid evaluation summary");
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
    public double acceptanceRate() { return total == 0 ? 0 : (double) accepted / total; }
    public boolean passes(double minimumAcceptanceRate) {
        return total > 0 && minimumAcceptanceRate >= 0 && minimumAcceptanceRate <= 1
                && acceptanceRate() >= minimumAcceptanceRate && schemaFailures == 0 && provenanceFailures == 0;
    }
}
