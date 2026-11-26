package org.modernash.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

/**
 * {@code CALL target [USING ...] [ON EXCEPTION ...] [NOT ON EXCEPTION ...]}.
 *
 * @param target              called program name (literal content) or, when {@code dynamic},
 *                            the data item holding it
 * @param arguments           USING operands in order
 * @param byReference         per argument: passed BY REFERENCE (the callee may change it)
 * @param dynamic             the target is a data item resolved at run time
 * @param exceptionStatements ON EXCEPTION / ON OVERFLOW statements (program not available)
 * @param normalStatements    NOT ON EXCEPTION statements
 */
public record CallStatement(String target, List<String> arguments, List<Boolean> byReference, boolean dynamic,
                            List<CobolStatement> exceptionStatements,
                            List<CobolStatement> normalStatements) implements CobolStatement {

    public CallStatement {
        target = Objects.requireNonNull(target, "target");
        arguments = List.copyOf(arguments == null ? List.of() : arguments);
        byReference = byReference == null || byReference.size() != arguments.size()
                ? arguments.stream().map(argument -> Boolean.TRUE).toList()
                : List.copyOf(byReference);
        exceptionStatements = List.copyOf(exceptionStatements == null ? List.of() : exceptionStatements);
        normalStatements = List.copyOf(normalStatements == null ? List.of() : normalStatements);
    }

    public CallStatement(String target, List<String> arguments) {
        this(target, arguments, null, false, List.of(), List.of());
    }
}
