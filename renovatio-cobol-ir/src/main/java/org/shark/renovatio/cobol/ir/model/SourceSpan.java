package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

/** One-based, inclusive source location used for stable diagnostics and action items. */
public record SourceSpan(String source, int startLine, int startColumn, int endLine, int endColumn) {

    public SourceSpan {
        Objects.requireNonNull(source, "source");
        if (source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        if (startLine < 1 || startColumn < 1 || endLine < 1 || endColumn < 1) {
            throw new IllegalArgumentException("source coordinates must be one-based");
        }
        if (endLine < startLine || endLine == startLine && endColumn < startColumn) {
            throw new IllegalArgumentException("source span end must not precede its start");
        }
    }
}
