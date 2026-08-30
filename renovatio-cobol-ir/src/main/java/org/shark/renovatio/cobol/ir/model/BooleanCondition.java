package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record BooleanCondition(
        CobolCondition left,
        BooleanOperator operator,
        CobolCondition right,
        SourceSpan sourceSpan) implements CobolCondition {

    public BooleanCondition {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(sourceSpan, "sourceSpan");
    }

    public enum BooleanOperator {
        AND,
        OR
    }
}
