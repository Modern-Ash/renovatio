package org.shark.renovatio.semantic.ir;

/** One-based, inclusive source location. */
public record SourceSpan(String sourcePath, int startLine, int startColumn, int endLine, int endColumn) {
    public SourceSpan {
        sourcePath = SemanticIdentity.path(sourcePath);
        if (startLine < 1 || startColumn < 1 || endLine < 1 || endColumn < 1) {
            throw new IllegalArgumentException("source coordinates must be one-based");
        }
        if (endLine < startLine || endLine == startLine && endColumn < startColumn) {
            throw new IllegalArgumentException("source span end must not precede its start");
        }
    }

    String identity() {
        return sourcePath + ":" + startLine + ":" + startColumn + ":" + endLine + ":" + endColumn;
    }
}
