package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public final class ComputeStatement implements CobolStatement {

    private final String target;
    private final String expression;

    public ComputeStatement(String target, String expression) {
        this.target = Objects.requireNonNull(target, "target");
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    public String getTarget() {
        return target;
    }

    public String getExpression() {
        return expression;
    }
}
