package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record LiteralExpression(LiteralKind kind, String value, SourceSpan sourceSpan)
        implements CobolExpression {

    public LiteralExpression {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(sourceSpan, "sourceSpan");
    }

    public enum LiteralKind {
        NUMERIC,
        ALPHANUMERIC,
        ZERO,
        SPACE
    }
}
