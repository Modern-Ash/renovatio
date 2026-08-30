package org.shark.renovatio.cobol.ir.annotated;

import java.util.List;
import java.util.Objects;

public record DataIntentPayload(Construction construction, String interpretation, List<String> assumptions)
        implements AnnotationPayload {
    public DataIntentPayload {
        Objects.requireNonNull(construction, "construction");
        interpretation = AnnotatedContract.text(interpretation, "interpretation");
        if (assumptions == null || assumptions.isEmpty()) throw new IllegalArgumentException("assumptions must not be empty");
        assumptions = assumptions.stream().map(value -> AnnotatedContract.text(value, "assumption")).toList();
    }

    public enum Construction { REDEFINES, OCCURS_DEPENDING_ON }
}
