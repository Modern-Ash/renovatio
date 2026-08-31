package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanAnnotationReviewServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HASH = "a".repeat(64);
    private static final String NODE = "b".repeat(64);
    private static final Instant NOW = Instant.parse("2026-08-31T00:30:00Z");
    private final HumanAnnotationReviewService service = new HumanAnnotationReviewService(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void specOwnerCanAcceptDataIntentWithoutChangingProposalIdentityOrPayload() {
        AnnotatedCobolModel pending = pendingDataIntent();
        var before = pending.annotations().get(0);

        AnnotatedCobolModel accepted = service.review(pending, before.annotationId(),
                HumanAnnotationReviewService.Decision.ACCEPT, "project:owner");

        var after = accepted.annotations().get(0);
        assertEquals(AnnotationReview.ReviewState.ACCEPTED, after.review().reviewState());
        assertEquals("project:owner", after.review().reviewedBy());
        assertEquals(NOW, after.review().reviewedAt());
        assertEquals(before.annotationId(), after.annotationId());
        assertSame(before.payload(), after.payload());
        assertSame(before.provenance(), after.provenance());
        assertTrue(service.isConsumable(after));
        assertFalse(service.isConsumable(before));
    }

    @Test
    void rejectionIsFinalAndNeverConsumable() {
        AnnotatedCobolModel pending = pendingDataIntent();
        AnnotatedCobolModel rejected = service.review(pending, pending.annotations().get(0).annotationId(),
                HumanAnnotationReviewService.Decision.REJECT, "project:owner");

        assertEquals(AnnotationReview.ReviewState.REJECTED,
                rejected.annotations().get(0).review().reviewState());
        assertFalse(service.isConsumable(rejected.annotations().get(0)));
        assertThrows(IllegalStateException.class, () -> service.review(rejected,
                rejected.annotations().get(0).annotationId(), HumanAnnotationReviewService.Decision.ACCEPT,
                "project:owner"));
    }

    @Test
    void providerAgentAndDeveloperCannotSupplyHumanConfirmation() {
        AnnotatedCobolModel pending = pendingDataIntent();
        String annotationId = pending.annotations().get(0).annotationId();
        for (String actor : List.of("provider:anthropic", "project:agent", "developer")) {
            assertThrows(SecurityException.class, () -> service.review(pending, annotationId,
                    HumanAnnotationReviewService.Decision.ACCEPT, actor));
        }
        assertEquals(AnnotationReview.ReviewState.NEEDS_REVIEW,
                pending.annotations().get(0).review().reviewState());
    }

    @Test
    void absentConfirmationCannotBeConsumed() {
        assertFalse(service.isConsumable(pendingDataIntent().annotations().get(0)));
    }

    @Test
    void specOwnerCanReviewDomainNamingProposal() {
        AnnotatedCobolModel pending = pendingDomainNaming();
        var proposal = pending.annotations().get(0);

        AnnotatedCobolModel accepted = service.review(pending, proposal.annotationId(),
                HumanAnnotationReviewService.Decision.ACCEPT, "project:owner");

        assertEquals(AnnotationReview.ReviewState.ACCEPTED,
                accepted.annotations().get(0).review().reviewState());
        assertTrue(service.isConsumable(accepted.annotations().get(0)));
    }

    private static AnnotatedCobolModel pendingDataIntent() {
        AnnotatedCobolModel empty = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", HASH, List.of());
        ResidualAnnotationContext context = new ResidualAnnotationContext("cobol-ir.v1", HASH, NODE,
                AnnotatedNodeKind.DATA_ITEM, "offline", "fake", "v1", "data-intent.v1", HASH,
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS, 0.75,
                "project:owner", List.of(), null, List.of(), false);
        var output = JSON.createObjectNode().put("construction", "REDEFINES")
                .put("interpretation", "Alternate view of the same storage");
        output.putArray("assumptions").add("Layouts intentionally overlap");
        return new ResidualAnnotationAssembler().append(empty, ResidualRoute.REDEFINES_INTENT,
                output, context);
    }

    private static AnnotatedCobolModel pendingDomainNaming() {
        AnnotatedCobolModel empty = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", HASH, List.of());
        ResidualAnnotationContext context = new ResidualAnnotationContext("cobol-ir.v1", HASH, NODE,
                AnnotatedNodeKind.PARAGRAPH, "offline", "fake", "v1", "domain-naming.v1", HASH,
                "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS, 0.75,
                "project:owner", List.of(), null, List.of(), false);
        var output = JSON.createObjectNode().put("suggestedName", "calculateInterest")
                .put("rationale", "Describes the paragraph action");
        return new ResidualAnnotationAssembler().append(empty, ResidualRoute.DOMAIN_NAMING,
                output, context);
    }
}
