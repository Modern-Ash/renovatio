package org.shark.renovatio.cobol.ir.annotated;

public sealed interface AnnotationPayload permits DomainNamingPayload, ControlFlowPlanPayload,
        DataIntentPayload, UnsupportedExplanationPayload {
}
