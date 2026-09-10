package org.shark.renovatio.llm.residual;

import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;
import org.shark.renovatio.llm.cache.CacheKey;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Stable, auditable manual work item emitted when deterministic translation is unavailable. */
public record ManualMigrationAction(
        String actionId,
        String nodeId,
        String construction,
        String reason,
        String semanticRisk,
        String humanAction,
        String evidenceRequired,
        String diagnosticCode,
        String toolRunRef) implements Comparable<ManualMigrationAction> {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern TOOL_RUN = Pattern.compile("tool-[0-9]{8}t[0-9]{14}z");

    public ManualMigrationAction {
        requireMatch(actionId, HASH, "actionId");
        requireMatch(nodeId, HASH, "nodeId");
        construction = text(construction, "construction");
        reason = text(reason, "reason");
        semanticRisk = text(semanticRisk, "semanticRisk");
        humanAction = text(humanAction, "humanAction");
        evidenceRequired = text(evidenceRequired, "evidenceRequired");
        diagnosticCode = text(diagnosticCode, "diagnosticCode");
        requireMatch(toolRunRef, TOOL_RUN, "toolRunRef");
        rejectUnsupportedPreservationClaim(reason + " " + semanticRisk + " " + humanAction);
        String expected = deriveId(nodeId, construction, reason, semanticRisk, humanAction,
                evidenceRequired, diagnosticCode, toolRunRef);
        if (!expected.equals(actionId)) throw new IllegalArgumentException("actionId does not match canonical content");
    }

    public static ManualMigrationAction create(String nodeId, String construction, String reason,
                                                String semanticRisk, String humanAction,
                                                String evidenceRequired, String diagnosticCode,
                                                String toolRunRef) {
        return new ManualMigrationAction(deriveId(nodeId, construction, reason, semanticRisk,
                humanAction, evidenceRequired, diagnosticCode, toolRunRef), nodeId, construction,
                reason, semanticRisk, humanAction, evidenceRequired, diagnosticCode, toolRunRef);
    }

    private static String deriveId(String nodeId, String construction, String reason,
                                   String semanticRisk, String humanAction, String evidenceRequired,
                                   String diagnosticCode, String toolRunRef) {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("nodeId", nodeId);
        projection.put("construction", construction);
        projection.put("reason", reason);
        projection.put("semanticRisk", semanticRisk);
        projection.put("humanAction", humanAction);
        projection.put("evidenceRequired", evidenceRequired);
        projection.put("diagnosticCode", diagnosticCode);
        projection.put("toolRunRef", toolRunRef);
        return CacheKey.sha256(CanonicalJson.write(projection));
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    private static void requireMatch(String value, Pattern pattern, String field) {
        text(value, field);
        if (!pattern.matcher(value).matches()) throw new IllegalArgumentException(field + " has invalid format");
    }

    private static void rejectUnsupportedPreservationClaim(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("behavior preserved") || normalized.contains("semantics preserved")
                || normalized.contains("preserved behavior") || normalized.contains("preserved semantics")) {
            throw new IllegalArgumentException("manual action cannot claim unverified semantic preservation");
        }
    }

    @Override
    public int compareTo(ManualMigrationAction other) {
        return actionId.compareTo(other.actionId);
    }
}
