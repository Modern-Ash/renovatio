package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record BinaryArithmeticExpression(
        CobolExpression left,
        ArithmeticOperator operator,
        CobolExpression right,
        SourceSpan sourceSpan) implements CobolExpression {

    public BinaryArithmeticExpression {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(sourceSpan, "sourceSpan");
    }

    public enum ArithmeticOperator {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE
    }
}
