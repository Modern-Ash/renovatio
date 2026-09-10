package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolValidator;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.ControlFlowPlanPayload;
import org.shark.renovatio.cobol.ir.annotated.DataIntentPayload;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidualAnnotationAssemblerTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HASH = "a".repeat(64);
    private static final String NODE = "b".repeat(64);
    private final ResidualAnnotationAssembler assembler = new ResidualAnnotationAssembler();

    @Test
    void domainOutputBecomesProposedTypedAnnotationWithValidIdentityAndProvenance() {
        AnnotatedCobolModel result = assembler.append(empty(), ResidualRoute.DOMAIN_NAMING,
                JSON.createObjectNode().put("suggestedName", "calculateInterest")
                        .put("boundedContext", "collections").put("rationale", "Paragraph calculates interest"),
                context(AnnotatedNodeKind.PARAGRAPH, List.of(), "project:owner"));

        var annotation = result.annotations().get(0);
        assertEquals(AnnotationFamily.DOMAIN_NAMING, annotation.annotationFamily());
        assertEquals(AnnotationReview.ReviewState.NEEDS_REVIEW, annotation.review().reviewState());
        assertEquals("cobol.domain.naming.v1", annotation.provenance().promptId());
        assertEquals(AnnotationProvenance.CacheDisposition.MISS, annotation.provenance().cacheDisposition());
        assertTrue(new AnnotatedCobolValidator().validate(result, HASH,
                Map.of(NODE, AnnotatedNodeKind.PARAGRAPH)).isEmpty());
    }

    @Test
    void controlFlowPlanUsesGovernedAffectedIdsAndRequiresHumanReview() {
        var output = JSON.createObjectNode();
        output.putArray("steps").add("Introduce explicit state");
        output.putArray("risks").add("Termination behavior");
        AnnotatedCobolModel result = assembler.append(empty(), ResidualRoute.CONTROL_FLOW_PLAN,
                output,
                context(AnnotatedNodeKind.PARAGRAPH, List.of(NODE), "project:owner"));

        var annotation = result.annotations().get(0);
        assertEquals(AnnotationReview.ReviewState.NEEDS_REVIEW, annotation.review().reviewState());
        assertEquals("project:owner", annotation.review().assignedReviewer());
        assertEquals(List.of(NODE), ((ControlFlowPlanPayload) annotation.payload()).affectedNodeIds());
        assertTrue(new AnnotatedCobolValidator().validate(result, HASH,
                Map.of(NODE, AnnotatedNodeKind.PARAGRAPH)).isEmpty());
    }

    @Test
    void dataIntentCannotClaimAConstructionDifferentFromItsRoute() {
        var output = JSON.createObjectNode().put("construction", "OCCURS_DEPENDING_ON")
                .put("interpretation", "overlay");
        output.putArray("assumptions").add("same storage");

        assertThrows(IllegalArgumentException.class, () -> assembler.append(empty(),
                ResidualRoute.REDEFINES_INTENT, output,
                context(AnnotatedNodeKind.DATA_ITEM, List.of(), "project:owner")));
    }

    @Test
    void dataIntentIsAlwaysPendingHumanReview() {
        var output = JSON.createObjectNode().put("construction", "REDEFINES")
                .put("interpretation", "Alternate storage view");
        output.putArray("assumptions").add("Layouts intentionally overlap");
        AnnotatedCobolModel result = assembler.append(empty(), ResidualRoute.REDEFINES_INTENT,
                output, context(AnnotatedNodeKind.DATA_ITEM, List.of(), "project:owner"));

        var annotation = result.annotations().get(0);
        assertEquals(AnnotationReview.ReviewState.NEEDS_REVIEW, annotation.review().reviewState());
        assertEquals(DataIntentPayload.Construction.REDEFINES,
                ((DataIntentPayload) annotation.payload()).construction());
    }

    @Test
    void moveCorrespondingIntentBecomesValidatedDataIntentAnnotationPendingReview() {
        var output = JSON.createObjectNode().put("construction", "MOVE_CORRESPONDING")
                .put("interpretation", "Copy customer contact fields by matching name");
        output.putArray("assumptions").add("Only identically named elementary items are copied");
        AnnotatedCobolModel result = assembler.append(empty(), ResidualRoute.MOVE_CORRESPONDING_INTENT,
                output, context(AnnotatedNodeKind.DATA_ITEM, List.of(), "project:owner"));

        var annotation = result.annotations().get(0);
        assertEquals(AnnotationFamily.DATA_INTENT, annotation.annotationFamily());
        assertEquals(AnnotationReview.ReviewState.NEEDS_REVIEW, annotation.review().reviewState());
        assertEquals(DataIntentPayload.Construction.MOVE_CORRESPONDING,
                ((DataIntentPayload) annotation.payload()).construction());
        assertTrue(new AnnotatedCobolValidator().validate(result, HASH,
                Map.of(NODE, AnnotatedNodeKind.DATA_ITEM)).isEmpty());
    }

    @Test
    void deterministicOutputAndMismatchedBaseIrCannotEnterSidecar() {
        assertThrows(IllegalArgumentException.class, () -> assembler.append(empty(),
                ResidualRoute.DETERMINISTIC, JSON.createObjectNode(),
                context(AnnotatedNodeKind.PARAGRAPH, List.of(), null)));
        ResidualAnnotationContext wrongBase = new ResidualAnnotationContext("cobol-ir.v1", "c".repeat(64),
                NODE, AnnotatedNodeKind.PARAGRAPH, "offline", "fake", "v1", "domain-naming.v1",
                HASH, "tool-20260830t12345678901234z", AnnotationProvenance.CacheDisposition.MISS,
                0.8, "project:owner", List.of(), null, List.of(), false);
        assertThrows(IllegalArgumentException.class, () -> assembler.append(empty(),
                ResidualRoute.DOMAIN_NAMING,
                JSON.createObjectNode().put("suggestedName", "name").put("rationale", "reason"), wrongBase));
    }

    private static AnnotatedCobolModel empty() {
        return new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION, "cobol-ir.v1", HASH, List.of());
    }

    private static ResidualAnnotationContext context(AnnotatedNodeKind kind, List<String> affected,
                                                     String reviewer) {
        return new ResidualAnnotationContext("cobol-ir.v1", HASH, NODE, kind, "offline", "fake",
                "v1", "annotated-output.v1", HASH, "tool-20260830t12345678901234z",
                AnnotationProvenance.CacheDisposition.MISS, 0.8, reviewer, affected,
                null, List.of(), false);
    }

    @Test
    void repeatedIdentityIsIdempotentAndConflictingOutputIsRejected() {
        var context = context(AnnotatedNodeKind.PARAGRAPH, List.of(), "project:owner");
        var firstOutput = JSON.createObjectNode().put("suggestedName", "calculateInterest")
                .put("rationale", "Describes the paragraph action");
        AnnotatedCobolModel first = assembler.append(empty(), ResidualRoute.DOMAIN_NAMING,
                firstOutput, context);

        assertSame(first, assembler.append(first, ResidualRoute.DOMAIN_NAMING, firstOutput, context));

        var conflicting = JSON.createObjectNode().put("suggestedName", "calculatePenalty")
                .put("rationale", "Different model output for the same identity");
        assertThrows(IllegalStateException.class, () -> assembler.append(first,
                ResidualRoute.DOMAIN_NAMING, conflicting, context));
    }
}
