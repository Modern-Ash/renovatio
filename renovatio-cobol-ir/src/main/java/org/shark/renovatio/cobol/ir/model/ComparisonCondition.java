package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record ComparisonCondition(
        CobolExpression left,
        ComparisonOperator operator,
        CobolExpression right,
        SourceSpan sourceSpan) implements CobolCondition {

    public ComparisonCondition {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(sourceSpan, "sourceSpan");
    }

    public enum ComparisonOperator {
        EQUAL,
        NOT_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL
    }
}
