package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record ComputeStatement(String target, String expression) implements CobolStatement {

    public ComputeStatement(String target, String expression) {
        this.target = Objects.requireNonNull(target, "target");
        this.expression = Objects.requireNonNull(expression, "expression");
    }
}
