package org.shark.renovatio.decisions;

import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.profile.ReusableAssetIdentifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable version of reusable decisions and its guarded match thresholds. */
public record DecisionPolicyCatalog(
        String schemaVersion,
        String name,
        String version,
        String signatureSchemaVersion,
        String analyzerVersion,
        BigDecimal autoConfirmThreshold,
        BigDecimal suggestThreshold,
        List<DecisionPolicyEntry> entries,
        String contentHash,
        Instant createdAt) {
    public static final String SCHEMA_VERSION = "1";

    public DecisionPolicyCatalog {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("schemaVersion must equal 1");
        ReusableAssetIdentifier.require(name, "name");
        ReusableAssetIdentifier.require(version, "version");
        if (!SemanticDecisionSignature.SCHEMA_VERSION.equals(signatureSchemaVersion))
            throw new IllegalArgumentException("unsupported signature schema version");
        if (analyzerVersion == null || analyzerVersion.isBlank()) throw new IllegalArgumentException("analyzerVersion is required");
        validateThresholds(autoConfirmThreshold, suggestThreshold);
        entries = entries == null ? List.of() : entries.stream()
                .sorted(Comparator.comparing(entry -> entry.signature().digest())).toList();
        if (contentHash == null || !contentHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("contentHash must be lowercase SHA-256");
        if (createdAt == null) throw new IllegalArgumentException("createdAt is required");
    }

    public static DecisionPolicyCatalog create(String name, String version, String analyzerVersion,
                                               BigDecimal autoConfirmThreshold, BigDecimal suggestThreshold,
                                               List<DecisionPolicyEntry> entries, Instant createdAt) {
        List<DecisionPolicyEntry> ordered = entries == null ? List.of() : entries.stream()
                .sorted(Comparator.comparing(entry -> entry.signature().digest())).toList();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("schemaVersion", SCHEMA_VERSION);
        content.put("name", name);
        content.put("version", version);
        content.put("signatureSchemaVersion", SemanticDecisionSignature.SCHEMA_VERSION);
        content.put("analyzerVersion", analyzerVersion);
        content.put("autoConfirmThreshold", autoConfirmThreshold);
        content.put("suggestThreshold", suggestThreshold);
        content.put("entries", ordered);
        return new DecisionPolicyCatalog(SCHEMA_VERSION, name, version, SemanticDecisionSignature.SCHEMA_VERSION,
                analyzerVersion, autoConfirmThreshold, suggestThreshold, ordered,
                MigrationProfiles.sha256(MigrationProfiles.canonical(content)), createdAt);
    }

    private static void validateThresholds(BigDecimal auto, BigDecimal suggest) {
        if (auto == null || suggest == null || auto.signum() < 0 || auto.compareTo(BigDecimal.ONE) > 0
                || suggest.signum() < 0 || suggest.compareTo(BigDecimal.ONE) > 0 || auto.compareTo(suggest) < 0)
            throw new IllegalArgumentException("thresholds must satisfy 0 <= suggest <= auto <= 1");
    }

    public PolicyReference reference() { return new PolicyReference(name, version); }
}
