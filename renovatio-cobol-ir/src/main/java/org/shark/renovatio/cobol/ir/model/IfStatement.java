package org.shark.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

public final class IfStatement implements CobolStatement {

    private final String condition;
    private final List<CobolStatement> thenStatements;
    private final List<CobolStatement> elseStatements;

    public IfStatement(String condition,
                       List<CobolStatement> thenStatements,
                       List<CobolStatement> elseStatements) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.thenStatements = List.copyOf(thenStatements == null ? List.of() : thenStatements);
        this.elseStatements = List.copyOf(elseStatements == null ? List.of() : elseStatements);
    }

    public String getCondition() {
        return condition;
    }

    public List<CobolStatement> getThenStatements() {
        return thenStatements;
    }

    public List<CobolStatement> getElseStatements() {
        return elseStatements;
    }
}
