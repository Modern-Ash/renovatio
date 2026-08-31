package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind;
import org.shark.renovatio.cobol.ir.annotated.AnnotationProvenance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualMigrationActionsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HASH = "a".repeat(64);
    private static final String NODE = "b".repeat(64);

    @Test
    void unsupportedExplanationProducesPreciseContentAddressedAction() {
        var annotation = unsupported().annotations().get(0);
        ManualMigrationActions actions = new ManualMigrationActions().addUnsupported(annotation,
                "LLM_UNSUPPORTED_CONSTRUCTION", "Characterization test and reviewed replacement diff");

        ManualMigrationAction action = actions.entries().get(0);
        assertEquals(NODE, action.nodeId());
        assertEquals("ALTER", action.construction());
        assertEquals("LLM_UNSUPPORTED_CONSTRUCTION", action.diagnosticCode());
        assertTrue(action.semanticRisk().contains(NODE));
        assertTrue(action.evidenceRequired().contains("Characterization test"));
        assertEquals(64, action.actionId().length());
    }

    @Test
    void identicalActionsAreDeduplicatedDeterministically() {
        var annotation = unsupported().annotations().get(0);
        ManualMigrationActions once = new ManualMigrationActions().addUnsupported(annotation,
                "LLM_UNSUPPORTED_CONSTRUCTION", "Reviewed replacement diff");
        ManualMigrationActions twice = once.addUnsupported(annotation,
                "LLM_UNSUPPORTED_CONSTRUCTION", "Reviewed replacement diff");

        assertEquals(1, twice.entries().size());
        assertEquals(once.entries().get(0).actionId(), twice.entries().get(0).actionId());
    }

    @Test
    void unverifiedPreservationClaimsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ManualMigrationAction.create(NODE, "ALTER",
                "Runtime target mutation is unsupported", "Semantics preserved by manual translation",
                "Replace with dispatch", "Green characterization test", "LLM_UNSUPPORTED_CONSTRUCTION",
                "tool-20260830t12345678901234z"));
    }

    @Test
    void nonUnsupportedAnnotationsCannotBecomeUnsupportedActions() {
        AnnotatedCobolModel empty = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", HASH, List.of());
        ResidualAnnotationContext context = context(AnnotatedNodeKind.PARAGRAPH);
        AnnotatedCobolModel domain = new ResidualAnnotationAssembler().append(empty,
                ResidualRoute.DOMAIN_NAMING,
                JSON.createObjectNode().put("suggestedName", "calculateInterest")
                        .put("rationale", "Matches supplied facts"), context);

        assertThrows(IllegalArgumentException.class, () -> new ManualMigrationActions().addUnsupported(
                domain.annotations().get(0), "CODE", "review"));
    }

    private static AnnotatedCobolModel unsupported() {
        AnnotatedCobolModel empty = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", HASH, List.of());
        var output = JSON.createObjectNode().put("construction", "ALTER")
                .put("explanation", "Runtime target mutation is unavailable in the deterministic lane")
                .put("manualAction", "Replace ALTER with explicit reviewed dispatch");
        return new ResidualAnnotationAssembler().append(empty, ResidualRoute.UNSUPPORTED_EXPLANATION,
                output, context(AnnotatedNodeKind.PARAGRAPH));
    }

    private static ResidualAnnotationContext context(AnnotatedNodeKind nodeKind) {
        return new ResidualAnnotationContext("cobol-ir.v1", HASH, NODE, nodeKind, "offline", "fake",
                "v1", "unsupported-explanation.v1", HASH, "tool-20260830t12345678901234z",
                AnnotationProvenance.CacheDisposition.MISS, 0.7, "project:owner", List.of(),
                null, List.of(), false);
    }
}
