package org.shark.renovatio.cobol.ir.model;

import lombok.Value;

import java.util.Objects;

@Value
public final class MoveStatement implements CobolStatement {

    String source;
    String target;

    public MoveStatement(String source, String target) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
    }
}
