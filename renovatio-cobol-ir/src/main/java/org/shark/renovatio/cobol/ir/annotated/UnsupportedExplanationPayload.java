package org.shark.renovatio.cobol.ir.annotated;

public record UnsupportedExplanationPayload(String construction, String explanation, String manualAction)
        implements AnnotationPayload {
    public UnsupportedExplanationPayload {
        construction = AnnotatedContract.text(construction, "construction");
        explanation = AnnotatedContract.text(explanation, "explanation");
        manualAction = AnnotatedContract.text(manualAction, "manualAction");
    }
}
