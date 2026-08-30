package org.shark.renovatio.cobol.ir.model;

import java.util.Locale;
import java.util.Objects;

public record DataReferenceExpression(String dataName, SourceSpan sourceSpan) implements CobolExpression {

    public DataReferenceExpression {
        Objects.requireNonNull(dataName, "dataName");
        if (dataName.isBlank()) {
            throw new IllegalArgumentException("dataName must not be blank");
        }
        dataName = dataName.toUpperCase(Locale.ROOT);
        Objects.requireNonNull(sourceSpan, "sourceSpan");
    }
}
