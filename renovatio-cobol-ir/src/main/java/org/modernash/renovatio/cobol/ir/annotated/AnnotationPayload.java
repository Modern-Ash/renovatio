package org.modernash.renovatio.cobol.ir.annotated;

public sealed interface AnnotationPayload permits DomainNamingPayload, ControlFlowPlanPayload,
        DataIntentPayload, UnsupportedExplanationPayload {
}
