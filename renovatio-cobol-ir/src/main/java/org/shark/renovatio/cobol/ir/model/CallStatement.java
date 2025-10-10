package org.shark.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

public final class CallStatement implements CobolStatement {

    private final String target;
    private final List<String> arguments;

    public CallStatement(String target, List<String> arguments) {
        this.target = Objects.requireNonNull(target, "target");
        this.arguments = List.copyOf(arguments == null ? List.of() : arguments);
    }

    public String getTarget() {
        return target;
    }

    public List<String> getArguments() {
        return arguments;
    }
}
