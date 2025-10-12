package org.shark.renovatio.cobol.ir.model;

import lombok.Value;

import java.util.List;
import java.util.Objects;

public record EvaluateStatement(String expression, List<EvaluateWhenBranch> branches) implements CobolStatement {

    public EvaluateStatement(String expression, List<EvaluateWhenBranch> branches) {
        this.expression = Objects.requireNonNull(expression, "expression");
        this.branches = List.copyOf(branches == null ? List.of() : branches);
    }

    @Value
    public static final class EvaluateWhenBranch {
        String condition;
        List<CobolStatement> statements;

        public EvaluateWhenBranch(String condition, List<CobolStatement> statements) {
            this.condition = Objects.requireNonNull(condition, "condition");
            this.statements = List.copyOf(statements == null ? List.of() : statements);
        }
    }
}
