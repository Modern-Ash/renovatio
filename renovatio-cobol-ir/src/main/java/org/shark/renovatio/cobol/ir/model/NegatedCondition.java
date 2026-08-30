package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record NegatedCondition(CobolCondition condition, SourceSpan sourceSpan) implements CobolCondition {

    public NegatedCondition {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(sourceSpan, "sourceSpan");
    }
}
