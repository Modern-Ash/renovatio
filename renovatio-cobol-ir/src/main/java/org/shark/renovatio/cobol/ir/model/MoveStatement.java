package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record MoveStatement(String source, String target) implements CobolStatement {

    public MoveStatement(String source, String target) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
    }
}
