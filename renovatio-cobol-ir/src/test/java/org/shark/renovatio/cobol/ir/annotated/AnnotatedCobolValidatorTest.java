package org.shark.renovatio.cobol.ir.annotated;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotatedCobolValidatorTest {

    private static final String BASE_HASH = "a".repeat(64);
    private static final String NODE_ID = "b".repeat(64);
    private static final String AFFECTED_ID = "c".repeat(64);
    private static final String ANNOTATION_ID = "d".repeat(64);

    private final AnnotatedCobolValidator validator = new AnnotatedCobolValidator();

    @Test
    void acceptsResolvedConsistentSidecar() {
        AnnotatedCobolModel sidecar = model(List.of(validAnnotation(NODE_ID, AFFECTED_ID)));

        assertTrue(validator.validate(sidecar, BASE_HASH,
                Map.of(NODE_ID, AnnotatedNodeKind.PARAGRAPH, AFFECTED_ID, AnnotatedNodeKind.PARAGRAPH)).isEmpty());
    }

    @Test
    void rejectsDeclaredHashesThatDoNotMatchCanonicalProjections() {
        CobolAnnotation malformed = annotation(ANNOTATION_ID, NODE_ID, AFFECTED_ID, "e".repeat(64));

        List<AnnotatedValidationDiagnostic> diagnostics = validator.validate(model(List.of(malformed)), BASE_HASH,
                Map.of(NODE_ID, AnnotatedNodeKind.PARAGRAPH, AFFECTED_ID, AnnotatedNodeKind.PARAGRAPH));

        assertEquals(List.of("/annotations/0/annotationId", "/annotations/0/provenance/outputHash"),
                diagnostics.stream().map(AnnotatedValidationDiagnostic::pointer).toList());
    }

    @Test
    void reportsStablePointersForCrossDocumentFailures() {
        CobolAnnotation first = annotation(ANNOTATION_ID, NODE_ID, AFFECTED_ID, "e".repeat(64));
        CobolAnnotation duplicate = annotation(ANNOTATION_ID, NODE_ID, AFFECTED_ID, "f".repeat(64));
        AnnotatedCobolModel sidecar = model(List.of(first, duplicate));

        List<AnnotatedValidationDiagnostic> diagnostics = validator.validate(sidecar, "0".repeat(64),
                Map.of(NODE_ID, AnnotatedNodeKind.DATA_ITEM));

        assertEquals(List.of(
                        "/annotations/0/annotationId",
                        "/annotations/0/nodeKind",
                        "/annotations/0/payload/affectedNodeIds/0",
                        "/annotations/0/provenance/outputHash",
                        "/annotations/1/annotationId",
                        "/annotations/1/annotationId",
                        "/annotations/1/nodeKind",
                        "/annotations/1/payload/affectedNodeIds/0",
                        "/annotations/1/provenance/outputHash",
                        "/annotations/1/provenance/outputHash",
                        "/baseIrHash"),
                diagnostics.stream().map(AnnotatedValidationDiagnostic::pointer).toList());
    }

    private AnnotatedCobolModel model(List<CobolAnnotation> annotations) {
        return new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION, "cobol-ir.v1",
                BASE_HASH, annotations);
    }

    private CobolAnnotation annotation(String annotationId, String nodeId, String affectedId, String outputHash) {
        AnnotationProvenance provenance = new AnnotationProvenance("offline", "fake", "cobol.goto.restructure",
                "v1", "control-flow.v1", "1".repeat(64), outputHash,
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS);
        return new CobolAnnotation(annotationId, nodeId, AnnotatedNodeKind.PARAGRAPH,
                AnnotationFamily.CONTROL_FLOW_PLAN,
                new ControlFlowPlanPayload(List.of(affectedId), List.of("Structure loop"), List.of("Behavior drift")),
                0.8, provenance, new AnnotationReview(AnnotationReview.ReviewState.PROPOSED, null, null, null));
    }

    private CobolAnnotation validAnnotation(String nodeId, String affectedId) {
        ControlFlowPlanPayload payload = new ControlFlowPlanPayload(List.of(affectedId),
                List.of("Structure loop"), List.of("Behavior drift"));
        AnnotationProvenance provisional = new AnnotationProvenance("offline", "fake", "cobol.goto.restructure",
                "v1", "control-flow.v1", "1".repeat(64), "0".repeat(64),
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS);
        String outputHash = AnnotatedIdentity.outputHash(AnnotationFamily.CONTROL_FLOW_PLAN, payload, 0.8);
        AnnotationProvenance provenance = new AnnotationProvenance(provisional.provider(), provisional.model(),
                provisional.promptId(), provisional.promptVersion(), provisional.outputSchemaVersion(),
                provisional.inputHash(), outputHash, provisional.toolRunRef(), provisional.cacheDisposition());
        String annotationId = AnnotatedIdentity.annotationId(nodeId, AnnotationFamily.CONTROL_FLOW_PLAN, provenance);
        return new CobolAnnotation(annotationId, nodeId, AnnotatedNodeKind.PARAGRAPH,
                AnnotationFamily.CONTROL_FLOW_PLAN, payload, 0.8, provenance,
                new AnnotationReview(AnnotationReview.ReviewState.PROPOSED, null, null, null));
    }
}
