package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

import lombok.Value;

@Value
public final class ComputeStatement implements CobolStatement {

    String target;
    String expression;

    public ComputeStatement(String target, String expression) {
        this.target = Objects.requireNonNull(target, "target");
        this.expression = Objects.requireNonNull(expression, "expression");
    }
}
