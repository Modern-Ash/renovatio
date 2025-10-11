package org.shark.renovatio.cobol.ir.model;

import lombok.Value;

import java.util.List;
import java.util.Objects;

@Value
public final class IfStatement implements CobolStatement {

    String condition;
    List<CobolStatement> thenStatements;
    List<CobolStatement> elseStatements;

    public IfStatement(String condition,
                       List<CobolStatement> thenStatements,
                       List<CobolStatement> elseStatements) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.thenStatements = List.copyOf(thenStatements == null ? List.of() : thenStatements);
        this.elseStatements = List.copyOf(elseStatements == null ? List.of() : elseStatements);
    }
}
