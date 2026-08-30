package org.shark.renovatio.cobol.ir.annotated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Domain-separated, content-addressed identities for annotated IR proposals and cache entries. */
public final class AnnotatedIdentity {

    private AnnotatedIdentity() {
    }

    public static String annotationId(String nodeId, AnnotationFamily family, AnnotationProvenance provenance) {
        return digest(identityProjection("annotation", nodeId, family, provenance));
    }

    public static String cacheKey(String nodeId, AnnotationFamily family, AnnotationProvenance provenance) {
        return digest(identityProjection("cache", nodeId, family, provenance));
    }

    public static String outputHash(AnnotationFamily family, AnnotationPayload payload, double confidence) {
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be finite and between 0 and 1");
        }
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("annotationFamily", family.name());
        projection.put("payload", payloadProjection(payload));
        projection.put("confidence", confidence);
        return digest(projection);
    }

    static String canonical(Object projection) {
        return CanonicalJson.write(projection);
    }

    static String hashCanonical(Object projection) {
        return digest(projection);
    }

    private static Map<String, Object> identityProjection(String identityType, String nodeId,
                                                           AnnotationFamily family,
                                                           AnnotationProvenance provenance) {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("identityType", identityType);
        projection.put("nodeId", AnnotatedContract.hash(nodeId, "nodeId"));
        projection.put("annotationFamily", family.name());
        projection.put("promptId", provenance.promptId());
        projection.put("promptVersion", provenance.promptVersion());
        projection.put("outputSchemaVersion", provenance.outputSchemaVersion());
        projection.put("inputHash", provenance.inputHash());
        return projection;
    }

    private static Map<String, Object> payloadProjection(AnnotationPayload payload) {
        Map<String, Object> projection = new LinkedHashMap<>();
        if (payload instanceof DomainNamingPayload value) {
            projection.put("suggestedName", value.suggestedName());
            projection.put("rationale", value.rationale());
            if (value.boundedContext() != null) projection.put("boundedContext", value.boundedContext());
        } else if (payload instanceof ControlFlowPlanPayload value) {
            projection.put("affectedNodeIds", value.affectedNodeIds());
            projection.put("steps", value.steps());
            projection.put("risks", value.risks());
        } else if (payload instanceof DataIntentPayload value) {
            projection.put("construction", value.construction().name());
            projection.put("interpretation", value.interpretation());
            projection.put("assumptions", value.assumptions());
        } else if (payload instanceof UnsupportedExplanationPayload value) {
            projection.put("construction", value.construction());
            projection.put("explanation", value.explanation());
            projection.put("manualAction", value.manualAction());
        } else {
            throw new IllegalArgumentException("Unsupported annotation payload: " + payload.getClass().getName());
        }
        return projection;
    }

    private static String digest(Object projection) {
        try {
            byte[] bytes = CanonicalJson.write(projection).getBytes(StandardCharsets.UTF_8);
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
