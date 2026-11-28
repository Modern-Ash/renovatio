package org.modernash.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

/**
 * COBOL {@code STRING}: sources, each up to its delimiter, are written one after the other into
 * the target from the pointer position; the rest of the target keeps its content.
 *
 * @param sources  operands in order
 * @param target   receiving item
 * @param pointer  WITH POINTER item (1-based, advanced past the last character written), or {@code null}
 */
public record StringStatement(List<Source> sources, String target, String pointer) implements CobolStatement {

    /**
     * One sending operand.
     *
     * @param operand    data item or literal
     * @param delimiter  DELIMITED BY operand (literal, SPACE, data item), {@code null} for SIZE
     */
    public record Source(String operand, String delimiter) {
        public Source {
            Objects.requireNonNull(operand, "operand");
        }
    }

    public StringStatement {
        sources = List.copyOf(sources);
        Objects.requireNonNull(target, "target");
    }
}
