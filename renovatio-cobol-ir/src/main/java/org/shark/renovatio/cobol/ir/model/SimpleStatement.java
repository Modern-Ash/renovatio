package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

/**
 * A PROCEDURE DIVISION statement with a fixed, argument-light shape: {@code DISPLAY},
 * {@code CONTINUE}, {@code GOBACK} / {@code STOP RUN}, or a line the parser recognises as a
 * statement but does not yet translate ({@link Kind#UNTRANSLATED}).
 *
 * <p>{@code text} holds the COBOL operand text (for {@code DISPLAY}) or the raw source line
 * (for {@code UNTRANSLATED}); it is empty for {@code CONTINUE} / {@code GOBACK} / {@code STOP_RUN}.
 */
public record SimpleStatement(Kind kind, String text) implements CobolStatement {

    public enum Kind {
        DISPLAY,
        CONTINUE,
        GOBACK,
        STOP_RUN,
        UNTRANSLATED
    }

    public SimpleStatement(Kind kind, String text) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.text = text == null ? "" : text.strip();
    }
}
