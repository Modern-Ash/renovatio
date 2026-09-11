package org.shark.renovatio.shared.llm;

import java.util.Map;

/**
 * Typed proposal returned by an LLM provider.
 * Versioned schema: typed-proposal.v1.json
 */
public record TypedProposal(
    String version,
    String kind,
    Map<String, Object> proposal,
    double confidence,
    String rationale,
    String schema,
    Metadata metadata
) {

    public record Metadata(
        String model,
        String promptHash,
        String inputHash,
        String outputHash,
        long latencyMs,
        int tokensUsed,
        String cacheStatus
    ) {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String version = "1.0";
        private String kind;
        private Map<String, Object> proposal;
        private double confidence;
        private String rationale;
        private String schema;
        private Metadata metadata;

        public Builder version(String v) { this.version = v; return this; }
        public Builder kind(String k) { this.kind = k; return this; }
        public Builder proposal(Map<String, Object> p) { this.proposal = p; return this; }
        public Builder confidence(double c) { this.confidence = c; return this; }
        public Builder rationale(String r) { this.rationale = r; return this; }
        public Builder schema(String s) { this.schema = s; return this; }
        public Builder metadata(Metadata m) { this.metadata = m; return this; }

        public TypedProposal build() {
            return new TypedProposal(version, kind, proposal, confidence, rationale, schema, metadata);
        }
    }
}