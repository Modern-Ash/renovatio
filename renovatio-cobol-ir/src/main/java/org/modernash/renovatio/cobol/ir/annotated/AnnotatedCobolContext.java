package org.modernash.renovatio.cobol.ir.annotated;

import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;

import java.util.Objects;

public record AnnotatedCobolContext(CobolIntermediateModel baseModel, AnnotatedCobolModel sidecar) {
    public static final String CONTEXT_KEY = "renovatio.cobol.annotated-ir";

    public AnnotatedCobolContext {
        Objects.requireNonNull(baseModel, "baseModel");
        Objects.requireNonNull(sidecar, "sidecar");
    }
}
