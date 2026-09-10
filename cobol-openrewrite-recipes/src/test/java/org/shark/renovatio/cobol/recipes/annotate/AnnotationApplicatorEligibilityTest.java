package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationApplicatorEligibilityTest {

    @Test
    void acceptsOnlyAcceptedAnnotationsWithMatchingHash() {
        AnnotatedFixtures.Fixture f = AnnotatedFixtures.domainNaming("clientFullName");

        AnnotationApplicator applicator = new AnnotationApplicator(f.model(), f.sidecar());

        assertThat(applicator.eligible()).hasSize(1);
    }

    @Test
    void rejectsStaleHash() {
        CobolIntermediateModel model = AnnotatedFixtures.model();
        AnnotatedFixtures.Fixture stale = AnnotatedFixtures.sidecar(model, "0".repeat(64),
                java.util.List.of(AnnotatedFixtures.domainNamingAnnotation(model, "clientFullName",
                        AnnotationReview.ReviewState.ACCEPTED)));

        assertThat(new AnnotationApplicator(stale.model(), stale.sidecar()).eligible()).isEmpty();
    }

    @Test
    void rejectsPendingAndProposedAndRejected() {
        for (AnnotationReview.ReviewState state : new AnnotationReview.ReviewState[]{
                AnnotationReview.ReviewState.PROPOSED,
                AnnotationReview.ReviewState.NEEDS_REVIEW,
                AnnotationReview.ReviewState.REJECTED}) {
            AnnotatedFixtures.Fixture f = AnnotatedFixtures.domainNaming("clientFullName", state);
            assertThat(new AnnotationApplicator(f.model(), f.sidecar()).eligible())
                    .as("state %s must not be eligible", state)
                    .isEmpty();
        }
    }

    @Test
    void reportsDroppedAnnotationsForIneligibleEntries() {
        AnnotatedFixtures.Fixture f = AnnotatedFixtures.domainNaming("clientFullName",
                AnnotationReview.ReviewState.REJECTED);

        var dropped = new AnnotationApplicator(f.model(), f.sidecar())
                .apply(null, new org.openrewrite.InMemoryExecutionContext()).dropped();

        assertThat(dropped).singleElement()
                .satisfies(d -> assertThat(d.reason()).isEqualTo(DroppedAnnotation.DropReason.REJECTED));
    }
}
