package org.shark.renovatio.cobol.recipes.annotate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Applies validated, {@code ACCEPTED} sidecar annotations to a generated {@link J.CompilationUnit}
 * using AST-safe transformations only. Ineligible annotations are reported as {@link DroppedAnnotation}
 * records; the tree is never left partially transformed.
 *
 * <p>The caller has already validated the sidecar (schema + semantic validator). This class only
 * re-checks the base-IR hash, the review state, the applied-family set, and per-node resolution.
 */
public final class AnnotationApplicator {

    private final AnnotatedCobolModel sidecar;
    private final NodeIdentityIndex index;
    private final boolean hashMatches;

    public AnnotationApplicator(CobolIntermediateModel model, AnnotatedCobolModel sidecar) {
        this.sidecar = sidecar;
        this.index = new NodeIdentityIndex(model);
        this.hashMatches = new CobolIrIdentityProjector().baseIrHash(model).equals(sidecar.baseIrHash());
    }

    List<CobolAnnotation> ordered() {
        List<CobolAnnotation> list = new ArrayList<>(sidecar.annotations());
        list.sort(Comparator.comparing(CobolAnnotation::nodeId).thenComparing(CobolAnnotation::annotationId));
        return list;
    }

    private static boolean isAppliedFamily(AnnotationFamily family) {
        return family == AnnotationFamily.DOMAIN_NAMING || family == AnnotationFamily.DATA_INTENT;
    }

    /** Annotations eligible for AST application, in deterministic {@code (nodeId, annotationId)} order. */
    List<CobolAnnotation> eligible() {
        List<CobolAnnotation> out = new ArrayList<>();
        if (!hashMatches) {
            return out;
        }
        for (CobolAnnotation a : ordered()) {
            if (a.review().reviewState() != AnnotationReview.ReviewState.ACCEPTED) {
                continue;
            }
            if (!isAppliedFamily(a.annotationFamily())) {
                continue;
            }
            if (index.resolve(a.nodeId(), a.nodeKind()).isEmpty()) {
                continue;
            }
            out.add(a);
        }
        return out;
    }

    public AnnotationApplicationOutcome apply(J.CompilationUnit cu, ExecutionContext ctx) {
        List<DroppedAnnotation> dropped = new ArrayList<>();
        for (CobolAnnotation a : ordered()) {
            classifyDrop(a).ifPresent(dropped::add);
        }
        // Tasks 4-5 add real mutation here for the eligible() set.
        return new AnnotationApplicationOutcome(cu, dropped);
    }

    private Optional<DroppedAnnotation> classifyDrop(CobolAnnotation a) {
        if (!hashMatches) {
            return drop(a, DroppedAnnotation.DropReason.STALE_SIDECAR);
        }
        if (!isAppliedFamily(a.annotationFamily())) {
            return drop(a, DroppedAnnotation.DropReason.FAMILY_NOT_APPLIED);
        }
        return switch (a.review().reviewState()) {
            case REJECTED -> drop(a, DroppedAnnotation.DropReason.REJECTED);
            case PROPOSED, NEEDS_REVIEW -> drop(a, DroppedAnnotation.DropReason.PENDING_REVIEW);
            case ACCEPTED -> index.resolve(a.nodeId(), a.nodeKind()).isEmpty()
                    ? drop(a, DroppedAnnotation.DropReason.NODE_UNRESOLVED)
                    : Optional.empty();
        };
    }

    private static Optional<DroppedAnnotation> drop(CobolAnnotation a, DroppedAnnotation.DropReason reason) {
        return Optional.of(new DroppedAnnotation(a.nodeId(), a.annotationId(), a.annotationFamily(),
                reason, a.annotationFamily().name()));
    }
}
