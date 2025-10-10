package org.shark.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

public final class EvaluateStatement implements CobolStatement {

    private final String expression;
    private final List<EvaluateWhenBranch> branches;

    public EvaluateStatement(String expression, List<EvaluateWhenBranch> branches) {
        this.expression = Objects.requireNonNull(expression, "expression");
        this.branches = List.copyOf(branches == null ? List.of() : branches);
    }

    public String getExpression() {
        return expression;
    }

    public List<EvaluateWhenBranch> getBranches() {
        return branches;
    }

    public static final class EvaluateWhenBranch {
        private final String condition;
        private final List<CobolStatement> statements;

        public EvaluateWhenBranch(String condition, List<CobolStatement> statements) {
            this.condition = Objects.requireNonNull(condition, "condition");
            this.statements = List.copyOf(statements == null ? List.of() : statements);
        }

        public String getCondition() {
            return condition;
        }

        public List<CobolStatement> getStatements() {
            return statements;
        }
    }
}
