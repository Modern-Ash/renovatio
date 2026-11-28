package org.modernash.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

/**
 * COBOL {@code UNSTRING}: the source is split at its delimiters and the fields are moved, in
 * order, into the receiving items; receivers beyond the fields found keep their content.
 *
 * @param source      sending item
 * @param delimiters  DELIMITED BY operands, each optionally prefixed with {@code ALL }
 * @param targets     INTO items in order
 */
public record UnstringStatement(String source, List<String> delimiters, List<String> targets)
        implements CobolStatement {

    public UnstringStatement {
        Objects.requireNonNull(source, "source");
        delimiters = List.copyOf(delimiters);
        targets = List.copyOf(targets);
    }
}
