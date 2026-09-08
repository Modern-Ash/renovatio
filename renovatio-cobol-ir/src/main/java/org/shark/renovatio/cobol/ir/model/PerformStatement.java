package org.shark.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

/**
 * A COBOL {@code PERFORM} statement.
 *
 * <p>Carries the optional paragraph range ({@code THRU}) and the optional loop / conditional
 * modifiers used by the {@code PERFORM} dialect:
 * <ul>
 *   <li>{@code PERFORM para n TIMES}</li>
 *   <li>{@code PERFORM para UNTIL condition}</li>
 *   <li>{@code PERFORM para VARYING var FROM start BY step UNTIL condition}</li>
 * </ul>
 * Any combination of {@code THRU} with the modifiers is allowed.
 *
 * <p>When the statement is an inline {@code PERFORM ... END-PERFORM} block the paragraph is
 * {@code null} and {@link #inlineBody()} carries the block statements. {@link #testAfter()}
 * reflects an explicit {@code WITH TEST AFTER} clause (default {@code false}, i.e. BEFORE).
 */
public record PerformStatement(
        String paragraph,
        String throughParagraph,
        String varyingVariable,
        String varyingFrom,
        String varyingBy,
        String untilCondition,
        Integer timesCount,
        boolean testAfter,
        List<CobolStatement> inlineBody) implements CobolStatement {

    public PerformStatement(String paragraph, String throughParagraph) {
        this(paragraph, throughParagraph, null, null, null, null, null, false, List.of());
    }

    public PerformStatement {
        paragraph = paragraph != null ? paragraph.toUpperCase() : null;
        throughParagraph = throughParagraph != null ? throughParagraph.toUpperCase() : null;
        varyingVariable = varyingVariable != null ? varyingVariable.toUpperCase() : null;
        varyingFrom = normalizeClause(varyingFrom);
        varyingBy = normalizeClause(varyingBy);
        untilCondition = normalizeClause(untilCondition);
        inlineBody = List.copyOf(inlineBody == null ? List.of() : inlineBody);
    }

    private static String normalizeClause(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public boolean hasVarying() {
        return varyingVariable != null && varyingFrom != null;
    }

    public boolean hasUntil() {
        return untilCondition != null;
    }

    public boolean hasTimes() {
        return timesCount != null;
    }

    /** True when this statement is an inline {@code PERFORM ... END-PERFORM} block. */
    public boolean isInline() {
        return paragraph == null && !inlineBody.isEmpty();
    }
}