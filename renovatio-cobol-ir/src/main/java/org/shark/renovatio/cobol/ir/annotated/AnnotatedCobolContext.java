package org.shark.renovatio.cobol.ir.annotated;

import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.util.Objects;

public record AnnotatedCobolContext(CobolIntermediateModel baseModel, AnnotatedCobolModel sidecar) {
    public static final String CONTEXT_KEY = "renovatio.cobol.annotated-ir";

    public AnnotatedCobolContext {
        Objects.requireNonNull(baseModel, "baseModel");
        Objects.requireNonNull(sidecar, "sidecar");
    }
}
