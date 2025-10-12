package org.shark.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

public record CallStatement(String target, List<String> arguments) implements CobolStatement {

    public CallStatement(String target, List<String> arguments) {
        this.target = Objects.requireNonNull(target, "target");
        this.arguments = List.copyOf(arguments == null ? List.of() : arguments);
    }
}
