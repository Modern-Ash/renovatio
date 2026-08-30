package org.shark.renovatio.cobol.ir.model;

/** Closed expression family accepted by deterministic translation. */
public sealed interface CobolExpression permits LiteralExpression, DataReferenceExpression,
        UnaryArithmeticExpression, BinaryArithmeticExpression {

    SourceSpan sourceSpan();
}
