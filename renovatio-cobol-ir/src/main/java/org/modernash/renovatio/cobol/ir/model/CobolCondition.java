package org.modernash.renovatio.cobol.ir.model;

/** Closed condition family accepted by deterministic translation. */
public sealed interface CobolCondition permits ComparisonCondition, BooleanCondition,
        NegatedCondition, Level88ConditionReference {

    SourceSpan sourceSpan();
}
