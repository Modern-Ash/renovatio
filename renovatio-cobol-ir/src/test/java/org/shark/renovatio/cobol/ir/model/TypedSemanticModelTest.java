package org.shark.renovatio.cobol.ir.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypedSemanticModelTest {

    private static final SourceSpan SPAN = new SourceSpan("sample.cob", 10, 8, 10, 32);

    @Test
    void buildsClosedArithmeticAndConditionTrees() {
        CobolExpression amount = new DataReferenceExpression("ws-amount", SPAN);
        CobolExpression expression = new BinaryArithmeticExpression(
                amount,
                BinaryArithmeticExpression.ArithmeticOperator.MULTIPLY,
                new LiteralExpression(LiteralExpression.LiteralKind.NUMERIC, "1.25", SPAN),
                SPAN);
        CobolCondition positive = new ComparisonCondition(
                expression,
                ComparisonCondition.ComparisonOperator.GREATER_THAN,
                new LiteralExpression(LiteralExpression.LiteralKind.ZERO, "0", SPAN),
                SPAN);
        CobolCondition combined = new BooleanCondition(
                positive,
                BooleanCondition.BooleanOperator.AND,
                new Level88ConditionReference("account-open", SPAN),
                SPAN);

        assertEquals("WS-AMOUNT", ((DataReferenceExpression)
                ((BinaryArithmeticExpression) expression).left()).dataName());
        assertEquals("ACCOUNT-OPEN", ((Level88ConditionReference)
                ((BooleanCondition) combined).right()).conditionName());
    }

    @Test
    void validatesSourceCoordinatesAndNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new SourceSpan("sample.cob", 2, 4, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new DataReferenceExpression(" ", SPAN));
        assertThrows(IllegalArgumentException.class,
                () -> new Level88ConditionReference("", SPAN));
    }

    @Test
    void ordersDiagnosticsByStableSourceLocation() {
        CobolDiagnostic later = diagnostic("COBOL-PERFORM-002", 20);
        CobolDiagnostic earlier = diagnostic("COBOL-IF-001", 12);

        assertEquals(List.of(earlier, later), List.of(later, earlier).stream().sorted().toList());
    }

    private static CobolDiagnostic diagnostic(String code, int line) {
        return new CobolDiagnostic(code, CobolDiagnostic.Severity.ERROR, "STATEMENT",
                "Unsupported deterministic form", new SourceSpan("sample.cob", line, 1, line, 20));
    }
}
