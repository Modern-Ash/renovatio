package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public final class MoveStatement implements CobolStatement {

    private final String source;
    private final String target;

    public MoveStatement(String source, String target) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }
}
