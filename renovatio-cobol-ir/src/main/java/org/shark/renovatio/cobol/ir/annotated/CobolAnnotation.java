package org.shark.renovatio.cobol.ir.annotated;

import java.util.Objects;

public record CobolAnnotation(String annotationId, String nodeId, AnnotatedNodeKind nodeKind,
                              AnnotationFamily annotationFamily, AnnotationPayload payload,
                              double confidence, AnnotationProvenance provenance, AnnotationReview review) {
    public CobolAnnotation {
        annotationId = AnnotatedContract.hash(annotationId, "annotationId");
        nodeId = AnnotatedContract.hash(nodeId, "nodeId");
        Objects.requireNonNull(nodeKind, "nodeKind");
        Objects.requireNonNull(annotationFamily, "annotationFamily");
        Objects.requireNonNull(payload, "payload");
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be finite and between 0 and 1");
        }
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(review, "review");
        if (!matches(annotationFamily, payload)) throw new IllegalArgumentException("payload does not match family");
    }

    private static boolean matches(AnnotationFamily family, AnnotationPayload payload) {
        return switch (family) {
            case DOMAIN_NAMING -> payload instanceof DomainNamingPayload;
            case CONTROL_FLOW_PLAN -> payload instanceof ControlFlowPlanPayload;
            case DATA_INTENT -> payload instanceof DataIntentPayload;
            case UNSUPPORTED_EXPLANATION -> payload instanceof UnsupportedExplanationPayload;
        };
    }
}
