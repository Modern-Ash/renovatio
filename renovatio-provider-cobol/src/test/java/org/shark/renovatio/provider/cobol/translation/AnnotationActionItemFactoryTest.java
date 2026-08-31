package org.shark.renovatio.provider.cobol.translation;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionSeverity;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationActionItemFactoryTest {

    private final AnnotationActionItemFactory factory = new AnnotationActionItemFactory();

    @Test
    void mapsRejectedToReviewEligibilityWarning() {
        DroppedAnnotation dropped = new DroppedAnnotation("n", "a", AnnotationFamily.DOMAIN_NAMING,
                DroppedAnnotation.DropReason.REJECTED, "rationale");

        ManualActionItem item = factory.toActionItem(dropped, "SAMPLE.cob", "SAMPLE");

        assertThat(item.id()).matches("^mai-[a-f0-9]{24}$");
        assertThat(item.failedGate()).isEqualTo(GuardrailGate.REVIEW_ELIGIBILITY);
        assertThat(item.severity()).isEqualTo(ManualActionSeverity.WARNING);
        assertThat(item.diagnosticReference()).isEqualTo("COBOL-ANNOTATION-REJECTED");
        assertThat(item.irNodeId()).isEqualTo("n");
    }

    @Test
    void mapsStaleSidecarToCharacterizationError() {
        DroppedAnnotation dropped = new DroppedAnnotation("n", "a", AnnotationFamily.DATA_INTENT,
                DroppedAnnotation.DropReason.STALE_SIDECAR, "DATA_INTENT");

        ManualActionItem item = factory.toActionItem(dropped, "SAMPLE.cob", "SAMPLE");

        assertThat(item.failedGate()).isEqualTo(GuardrailGate.CHARACTERIZATION);
        assertThat(item.severity()).isEqualTo(ManualActionSeverity.ERROR);
        assertThat(item.diagnosticReference()).isEqualTo("COBOL-ANNOTATION-STALE");
    }

    @Test
    void mapsStaleResolutionDiagnosticToCharacterizationError() {
        ManualActionItem item = factory.toResolutionDiagnostic(
                "sample.annotated.json: baseIrHash does not match the current COBOL IR",
                "sample.cob", "SAMPLE");

        assertThat(item.failedGate()).isEqualTo(GuardrailGate.CHARACTERIZATION);
        assertThat(item.severity()).isEqualTo(ManualActionSeverity.ERROR);
        assertThat(item.diagnosticReference()).isEqualTo("COBOL-ANNOTATION-STALE");
        assertThat(item.constructionFamily()).isEqualTo("ANNOTATED_SIDECAR");
    }

    @Test
    void mapsInvalidResolutionDiagnosticToSchemaError() {
        ManualActionItem item = factory.toResolutionDiagnostic(
                "sample.annotated.json: schema: $.annotations is required",
                "sample.cob", "SAMPLE");

        assertThat(item.failedGate()).isEqualTo(GuardrailGate.SCHEMA);
        assertThat(item.severity()).isEqualTo(ManualActionSeverity.ERROR);
        assertThat(item.diagnosticReference()).isEqualTo("COBOL-ANNOTATED-SIDECAR-INVALID");
    }
}
