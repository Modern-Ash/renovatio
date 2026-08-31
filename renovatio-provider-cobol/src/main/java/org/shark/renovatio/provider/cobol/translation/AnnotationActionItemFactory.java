package org.shark.renovatio.provider.cobol.translation;

import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItemIds;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionReviewStatus;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionSeverity;

/** Maps recipe-neutral annotation outcomes onto the versioned manual-action-item contract. */
public final class AnnotationActionItemFactory {

    public ManualActionItem toResolutionDiagnostic(String diagnostic, String sourceFile, String programId) {
        boolean stale = diagnostic.contains("baseIrHash");
        GuardrailGate gate = stale ? GuardrailGate.CHARACTERIZATION : GuardrailGate.SCHEMA;
        String code = stale ? "COBOL-ANNOTATION-STALE" : "COBOL-ANNOTATED-SIDECAR-INVALID";
        String reason = stale
                ? "Annotated sidecar does not match current IR: " + diagnostic
                : "Annotated sidecar failed validation: " + diagnostic;
        String family = "ANNOTATED_SIDECAR";
        String id = ManualActionItemIds.from(sourceFile, programId, diagnostic, family, reason);
        return new ManualActionItem(id, sourceFile, programId, null, null, null, null,
                null, null, family, reason, gate, code,
                "Invalid annotated input ignored; deterministic base translation retained",
                "Review the diagnostic and regenerate or repair the annotated sidecar",
                "Sidecar passes schema and semantic validation and matches the current base IR",
                ManualActionSeverity.ERROR, ManualActionReviewStatus.PENDING,
                null, null, null, null, null, null);
    }

    public ManualActionItem toActionItem(DroppedAnnotation dropped, String sourceFile, String programId) {
        Mapping mapping = switch (dropped.reason()) {
            case REJECTED -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.WARNING,
                    "COBOL-ANNOTATION-REJECTED",
                    "Rejected annotation not applied; deterministic translation retained");
            case PENDING_REVIEW -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.WARNING,
                    "COBOL-ANNOTATION-PENDING",
                    "Annotation pending human review; not eligible for deterministic application");
            case STALE_SIDECAR -> new Mapping(GuardrailGate.CHARACTERIZATION, ManualActionSeverity.ERROR,
                    "COBOL-ANNOTATION-STALE",
                    "Annotated sidecar does not match current IR; regenerate the sidecar");
            case NAME_COLLISION -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.WARNING,
                    "COBOL-DOMAIN-RENAME-COLLISION",
                    "Domain rename collides with an existing identifier in scope");
            case NODE_UNRESOLVED -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.ERROR,
                    "COBOL-ANNOTATION-NODE-UNRESOLVED",
                    "Annotation node cannot be resolved against the current IR");
            case FAMILY_NOT_APPLIED -> new Mapping(GuardrailGate.REVIEW_ELIGIBILITY, ManualActionSeverity.WARNING,
                    "COBOL-ANNOTATION-FAMILY-DEFERRED",
                    "Annotation family not applied deterministically; recorded for manual action");
        };
        String family = dropped.family().name();
        String id = ManualActionItemIds.from(sourceFile, programId, dropped.nodeId(), family, mapping.reason());
        return new ManualActionItem(id, sourceFile, programId, null, null, null, null,
                dropped.nodeId(), null, family, mapping.reason(), mapping.gate(), mapping.code(),
                "No annotated transformation applied for the affected node",
                "Human review of annotation " + dropped.annotationId(),
                "Annotation is accepted, matches current IR, and applies without collision",
                mapping.severity(), ManualActionReviewStatus.PENDING,
                null, null, null, null, null, null);
    }

    private record Mapping(GuardrailGate gate, ManualActionSeverity severity, String code, String reason) {
    }
}
