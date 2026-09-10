package org.shark.renovatio.cobol.recipes.annotate;

import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;

/**
 * A sidecar annotation the deterministic pass did not apply. Carried out of the recipe module as a
 * neutral record; {@code renovatio-provider-cobol} maps it to a {@code manual-action-item.v1} entry.
 */
public record DroppedAnnotation(String nodeId, String annotationId,
                                AnnotationFamily family, DropReason reason, String detail) {

    public enum DropReason {
        REJECTED, PENDING_REVIEW, STALE_SIDECAR, NAME_COLLISION, NODE_UNRESOLVED, FAMILY_NOT_APPLIED
    }
}
