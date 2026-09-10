package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record UnaryArithmeticExpression(
        UnaryOperator operator, CobolExpression operand, SourceSpan sourceSpan) implements CobolExpression {

    public UnaryArithmeticExpression {
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(operand, "operand");
        Objects.requireNonNull(sourceSpan, "sourceSpan");
    }

    public enum UnaryOperator {
        PLUS,
        MINUS
    }
}
