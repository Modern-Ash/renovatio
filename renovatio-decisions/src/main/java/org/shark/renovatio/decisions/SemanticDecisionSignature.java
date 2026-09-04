package org.shark.renovatio.decisions;

import org.shark.renovatio.profile.MigrationProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Location-independent semantic identity used to reuse decisions across projects. */
public record SemanticDecisionSignature(
        String schemaVersion,
        String analyzerVersion,
        DecisionPoint.Category category,
        String decisionKey,
        List<String> optionVocabulary,
        String nodeKind,
        Map<String, String> features,
        String digest) {

    public static final String SCHEMA_VERSION = "1";

    public SemanticDecisionSignature {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("signature schemaVersion must equal 1");
        if (analyzerVersion == null || analyzerVersion.isBlank()) throw new IllegalArgumentException("analyzerVersion is required");
        if (category == null) throw new IllegalArgumentException("category is required");
        if (decisionKey == null || decisionKey.isBlank()) throw new IllegalArgumentException("decisionKey is required");
        optionVocabulary = optionVocabulary == null ? List.of() : List.copyOf(optionVocabulary);
        if (optionVocabulary.size() < 2) throw new IllegalArgumentException("optionVocabulary requires at least two values");
        if (nodeKind == null || nodeKind.isBlank()) throw new IllegalArgumentException("nodeKind is required");
        features = Collections.unmodifiableMap(new TreeMap<>(features == null ? Map.of() : features));
        if (digest == null || !digest.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("digest must be lowercase SHA-256");
    }

    public static SemanticDecisionSignature create(DecisionPoint decision, String analyzerVersion,
                                                   Map<String, String> semanticFeatures) {
        return create(decision.category(), decision.decisionKey(), decision.options(),
                decision.location().nodeKind(), analyzerVersion, semanticFeatures);
    }

    public static SemanticDecisionSignature create(DecisionPoint.Category category, String decisionKey,
                                                   List<String> options, String nodeKind,
                                                   String analyzerVersion, Map<String, String> semanticFeatures) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("schemaVersion", SCHEMA_VERSION);
        content.put("analyzerVersion", analyzerVersion);
        content.put("category", category);
        content.put("decisionKey", decisionKey);
        content.put("optionVocabulary", options);
        content.put("nodeKind", nodeKind);
        content.put("features", new TreeMap<>(semanticFeatures == null ? Map.of() : semanticFeatures));
        return new SemanticDecisionSignature(SCHEMA_VERSION, analyzerVersion, category, decisionKey,
                options, nodeKind, semanticFeatures,
                MigrationProfiles.sha256(MigrationProfiles.canonical(content)));
    }

    public boolean compatibleWith(SemanticDecisionSignature other) {
        return other != null && category == other.category && decisionKey.equals(other.decisionKey)
                && optionVocabulary.equals(other.optionVocabulary) && nodeKind.equals(other.nodeKind);
    }

    /** Deterministic exact-value Jaccard score for normalized semantic feature maps. */
    public BigDecimal similarity(SemanticDecisionSignature other) {
        if (!compatibleWith(other)) return BigDecimal.ZERO;
        if (features.isEmpty() && other.features.isEmpty()) return BigDecimal.ONE;
        var keys = new java.util.HashSet<>(features.keySet());
        keys.addAll(other.features.keySet());
        long equal = keys.stream().filter(key -> java.util.Objects.equals(features.get(key), other.features.get(key))).count();
        return BigDecimal.valueOf(equal).divide(BigDecimal.valueOf(keys.size()), 5, RoundingMode.HALF_UP);
    }
}
