package org.shark.renovatio.decisions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Immutable v1 decision record; persistence adapters key it by project id plus id. */
public record DecisionPoint(
        String schemaVersion,
        String id,
        Category category,
        String decisionKey,
        Location location,
        String question,
        List<String> options,
        String defaultOption,
        String chosenOption,
        Source source,
        BigDecimal confidence,
        String rationale,
        List<String> evidence,
        Status status,
        String semanticIrHash,
        boolean llmFailed,
        LlmFailureCategory llmFailureCategory,
        long revision,
        Instant createdAt,
        Instant updatedAt,
        boolean active) {

    public static final String SCHEMA_VERSION = "1";

    public DecisionPoint {
        require(SCHEMA_VERSION.equals(schemaVersion), "schemaVersion must equal 1");
        require(id != null && id.matches("[0-9a-f]{64}"), "id must be lowercase SHA-256");
        Objects.requireNonNull(category, "category");
        require(nonBlank(decisionKey), "decisionKey is required");
        Objects.requireNonNull(location, "location");
        require(nonBlank(question), "question is required");
        options = options == null ? List.of() : List.copyOf(options);
        require(options.size() >= 2 && options.size() <= 20, "options must contain 2 through 20 values");
        require(options.stream().allMatch(DecisionPoint::nonBlank), "options must be non-blank");
        require(new HashSet<>(options).size() == options.size(), "options must be unique");
        require(options.contains(defaultOption), "defaultOption must belong to options");
        require(options.contains(chosenOption), "chosenOption must belong to options");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(confidence, "confidence");
        require(confidence.compareTo(BigDecimal.ZERO) >= 0 && confidence.compareTo(BigDecimal.ONE) <= 0,
                "confidence must be between 0 and 1");
        require(nonBlank(rationale) && rationale.length() <= 4_000, "rationale must be 1 through 4000 characters");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        require(evidence.stream().allMatch(DecisionPoint::nonBlank), "evidence entries must be non-blank");
        Objects.requireNonNull(status, "status");
        require(semanticIrHash != null && semanticIrHash.matches("[0-9a-f]{64}"),
                "semanticIrHash must be lowercase SHA-256");
        require(llmFailed == (llmFailureCategory != null), "LLM failure flag and category must agree");
        require(revision > 0, "revision must be positive");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        require(source != Source.USER || status == Status.OVERRIDDEN,
                "USER source is valid only for OVERRIDDEN decisions");
    }

    private static boolean nonBlank(String value) { return value != null && !value.isBlank(); }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public enum Category { NUMERIC, CONTROL_FLOW, DATA_SHAPE, PERSISTENCE, NAMING, ARCHITECTURE, BATCH }
    public enum Source { HEURISTIC, LLM, USER }
    public enum Status { AUTO, SUGGESTED, CONFIRMED, OVERRIDDEN }
    public enum LlmFailureCategory {
        PROVIDER_ERROR, ATTRIBUTION_ERROR, TIMEOUT, MALFORMED_JSON,
        SCHEMA_INVALID, OPTION_INVALID, SANITIZATION_FAILED, CACHE_ERROR
    }

    public record Location(String programId, String nodeKind, String nodeId) {
        public Location {
            require(nonBlank(programId), "programId is required");
            require(nonBlank(nodeKind), "nodeKind is required");
            require(nonBlank(nodeId), "nodeId is required");
        }
        public static Location project() { return new Location("project", "PROJECT", "project"); }
    }
}
