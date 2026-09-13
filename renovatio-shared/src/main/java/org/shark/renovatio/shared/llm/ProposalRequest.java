package org.shark.renovatio.shared.llm;

import java.util.Map;

/**
 * Request sent to an LLM provider for generating a typed proposal.
 * Versioned schema: proposal-request.v1.json
 */
public record ProposalRequest(
    String version,
    String kind,
    Input input,
    String schema,
    Budget budget,
    Model model
) {

    public record Input(
        String sourceHash,
        Map<String, Object> context
    ) {}

    public record Budget(
        int maxTokens,
        int maxLatencyMs,
        double maxCostUSD
    ) {}

    public record Model(
        String name,
        String version
    ) {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String version = "1.0";
        private String kind;
        private Input input;
        private String schema;
        private Budget budget;
        private Model model;

        public Builder version(String v) { this.version = v; return this; }
        public Builder kind(String k) { this.kind = k; return this; }
        public Builder input(Input i) { this.input = i; return this; }
        public Builder schema(String s) { this.schema = s; return this; }
        public Builder budget(Budget b) { this.budget = b; return this; }
        public Builder model(Model m) { this.model = m; return this; }

        public ProposalRequest build() {
            return new ProposalRequest(version, kind, input, schema, budget, model);
        }
    }
}