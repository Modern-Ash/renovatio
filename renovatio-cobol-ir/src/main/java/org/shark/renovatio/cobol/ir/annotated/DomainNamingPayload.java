package org.shark.renovatio.cobol.ir.annotated;

public record DomainNamingPayload(String suggestedName, String boundedContext, String rationale)
        implements AnnotationPayload {
    public DomainNamingPayload {
        suggestedName = AnnotatedContract.text(suggestedName, "suggestedName");
        if (boundedContext != null) boundedContext = AnnotatedContract.text(boundedContext, "boundedContext");
        rationale = AnnotatedContract.text(rationale, "rationale");
    }
}
