package org.shark.renovatio.cobol.ir.annotated;

import java.time.Instant;
import java.util.Objects;

public record AnnotationReview(ReviewState reviewState, String assignedReviewer, String reviewedBy, Instant reviewedAt) {
    public AnnotationReview {
        Objects.requireNonNull(reviewState, "reviewState");
        if (assignedReviewer != null) assignedReviewer = AnnotatedContract.text(assignedReviewer, "assignedReviewer");
        if (reviewedBy != null) reviewedBy = AnnotatedContract.text(reviewedBy, "reviewedBy");
        switch (reviewState) {
            case PROPOSED -> requireAbsent(assignedReviewer, reviewedBy, reviewedAt);
            case NEEDS_REVIEW -> {
                if (reviewedBy != null || reviewedAt != null) throw new IllegalArgumentException("pending review cannot be completed");
            }
            case ACCEPTED, REJECTED -> {
                if (assignedReviewer != null || reviewedBy == null || reviewedAt == null) {
                    throw new IllegalArgumentException("final review requires reviewer/time and no assignee");
                }
            }
        }
    }

    private static void requireAbsent(Object... values) {
        for (Object value : values) if (value != null) throw new IllegalArgumentException("proposed review has actor metadata");
    }

    public enum ReviewState { PROPOSED, NEEDS_REVIEW, ACCEPTED, REJECTED }
}
