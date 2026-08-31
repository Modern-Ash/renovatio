package org.shark.renovatio.llm.residual;

import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Human-only immutable review transition for ambiguous semantic proposals. */
public final class HumanAnnotationReviewService {
    public static final String SPEC_OWNER_ACTOR = "project:owner";

    private final Clock clock;

    public HumanAnnotationReviewService(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public AnnotatedCobolModel review(AnnotatedCobolModel sidecar, String annotationId,
                                      Decision decision, String actor) {
        Objects.requireNonNull(sidecar, "sidecar");
        Objects.requireNonNull(annotationId, "annotationId");
        Objects.requireNonNull(decision, "decision");
        if (!SPEC_OWNER_ACTOR.equals(actor)) {
            throw new SecurityException("only the assigned human spec owner may review semantic proposals");
        }

        List<CobolAnnotation> annotations = new ArrayList<>(sidecar.annotations());
        int index = find(annotations, annotationId);
        CobolAnnotation existing = annotations.get(index);
        if (existing.annotationFamily() != AnnotationFamily.DATA_INTENT
                && existing.annotationFamily() != AnnotationFamily.CONTROL_FLOW_PLAN) {
            throw new IllegalArgumentException("annotation family does not require semantic confirmation");
        }
        AnnotationReview current = existing.review();
        if (current.reviewState() != AnnotationReview.ReviewState.NEEDS_REVIEW
                || !SPEC_OWNER_ACTOR.equals(current.assignedReviewer())) {
            throw new IllegalStateException("annotation is not pending review by the assigned spec owner");
        }
        Instant reviewedAt = clock.instant();
        AnnotationReview.ReviewState state = decision == Decision.ACCEPT
                ? AnnotationReview.ReviewState.ACCEPTED : AnnotationReview.ReviewState.REJECTED;
        AnnotationReview reviewed = new AnnotationReview(state, null, actor, reviewedAt);
        annotations.set(index, new CobolAnnotation(existing.annotationId(), existing.nodeId(),
                existing.nodeKind(), existing.annotationFamily(), existing.payload(), existing.confidence(),
                existing.provenance(), reviewed));
        return new AnnotatedCobolModel(sidecar.schemaVersion(), sidecar.baseIrVersion(),
                sidecar.baseIrHash(), annotations);
    }

    public boolean isConsumable(CobolAnnotation annotation) {
        Objects.requireNonNull(annotation, "annotation");
        return annotation.review().reviewState() == AnnotationReview.ReviewState.ACCEPTED
                && (annotation.annotationFamily() == AnnotationFamily.DATA_INTENT
                || annotation.annotationFamily() == AnnotationFamily.CONTROL_FLOW_PLAN);
    }

    private static int find(List<CobolAnnotation> annotations, String annotationId) {
        for (int index = 0; index < annotations.size(); index++) {
            if (annotations.get(index).annotationId().equals(annotationId)) return index;
        }
        throw new IllegalArgumentException("annotation does not exist in the supplied sidecar");
    }

    public enum Decision { ACCEPT, REJECT }
}
