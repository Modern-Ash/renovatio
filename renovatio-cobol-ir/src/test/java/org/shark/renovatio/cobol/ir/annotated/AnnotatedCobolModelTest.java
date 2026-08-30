package org.shark.renovatio.cobol.ir.annotated;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnotatedCobolModelTest {
    private static final String HASH = "a".repeat(64);

    @Test
    void modelAndContext_shouldBeImmutableAndPreserveExactBaseReference() {
        List<CobolAnnotation> annotations = new ArrayList<>();
        AnnotatedCobolModel sidecar = new AnnotatedCobolModel(
                AnnotatedCobolModel.SCHEMA_VERSION, "cobol-ir.v1", HASH, annotations);
        annotations.add(annotation(new DomainNamingPayload("calculateInterest", "collections", "domain language")));
        CobolIntermediateModel base = CobolIntermediateModel.builder().programId("TEST").build();
        AnnotatedCobolContext context = new AnnotatedCobolContext(base, sidecar);

        assertTrue(sidecar.annotations().isEmpty());
        assertSame(base, context.baseModel());
        assertSame(sidecar, context.sidecar());
        assertEquals("renovatio.cobol.annotated-ir", AnnotatedCobolContext.CONTEXT_KEY);
    }

    @Test
    void annotation_shouldEnforceFamilyConfidenceAndProvenance() {
        assertThrows(IllegalArgumentException.class, () -> new CobolAnnotation(HASH, HASH,
                AnnotatedNodeKind.PARAGRAPH, AnnotationFamily.DATA_INTENT,
                new DomainNamingPayload("name", null, "reason"), 0.5, provenance(), proposed()));
        assertThrows(IllegalArgumentException.class, () -> new CobolAnnotation(HASH, HASH,
                AnnotatedNodeKind.PARAGRAPH, AnnotationFamily.DOMAIN_NAMING,
                new DomainNamingPayload("name", null, "reason"), Double.NaN, provenance(), proposed()));
        assertThrows(IllegalArgumentException.class, () -> new AnnotationProvenance("openai", "model",
                "prompt", "v1", "v1", HASH, HASH, "bad-run", AnnotationProvenance.CacheDisposition.MISS));
    }

    @Test
    void review_shouldEnforceSnapshotInvariants() {
        assertDoesNotThrow(() -> new AnnotationReview(AnnotationReview.ReviewState.NEEDS_REVIEW,
                "human:owner", null, null));
        assertDoesNotThrow(() -> new AnnotationReview(AnnotationReview.ReviewState.ACCEPTED,
                null, "human:owner", Instant.parse("2026-08-30T17:00:00Z")));
        assertThrows(IllegalArgumentException.class, () -> new AnnotationReview(
                AnnotationReview.ReviewState.PROPOSED, "human:owner", null, null));
        assertThrows(IllegalArgumentException.class, () -> new AnnotationReview(
                AnnotationReview.ReviewState.REJECTED, "human:owner", "human:owner", Instant.now()));
    }

    @Test
    void payloads_shouldRejectEmptyRequiredValues() {
        assertThrows(IllegalArgumentException.class, () -> new ControlFlowPlanPayload(List.of(), List.of("step"), List.of("risk")));
        assertThrows(IllegalArgumentException.class, () -> new DataIntentPayload(
                DataIntentPayload.Construction.REDEFINES, "intent", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new UnsupportedExplanationPayload("GO TO", "", "manual"));
    }

    private static CobolAnnotation annotation(AnnotationPayload payload) {
        return new CobolAnnotation(HASH, HASH, AnnotatedNodeKind.PARAGRAPH,
                AnnotationFamily.DOMAIN_NAMING, payload, 0.75, provenance(), proposed());
    }

    private static AnnotationProvenance provenance() {
        return new AnnotationProvenance("openai", "configured", "domain.name", "v1", "v1",
                HASH, HASH, "tool-20260830t17000000000000z", AnnotationProvenance.CacheDisposition.MISS);
    }

    private static AnnotationReview proposed() {
        return new AnnotationReview(AnnotationReview.ReviewState.PROPOSED, null, null, null);
    }
}
