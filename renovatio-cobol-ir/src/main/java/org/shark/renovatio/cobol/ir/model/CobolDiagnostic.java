package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

/** Structured deterministic refusal for recognized syntax outside the supported subset. */
public record CobolDiagnostic(
        String code,
        Severity severity,
        String constructionFamily,
        String message,
        SourceSpan sourceSpan) implements Comparable<CobolDiagnostic> {

    public CobolDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(constructionFamily, "constructionFamily");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(sourceSpan, "sourceSpan");
        if (code.isBlank() || constructionFamily.isBlank() || message.isBlank()) {
            throw new IllegalArgumentException("diagnostic text fields must not be blank");
        }
    }

    @Override
    public int compareTo(CobolDiagnostic other) {
        int bySource = sourceSpan.source().compareTo(other.sourceSpan.source());
        if (bySource != 0) {
            return bySource;
        }
        int byLine = Integer.compare(sourceSpan.startLine(), other.sourceSpan.startLine());
        if (byLine != 0) {
            return byLine;
        }
        int byColumn = Integer.compare(sourceSpan.startColumn(), other.sourceSpan.startColumn());
        return byColumn != 0 ? byColumn : code.compareTo(other.code);
    }

    public enum Severity {
        WARNING,
        ERROR
    }
}
