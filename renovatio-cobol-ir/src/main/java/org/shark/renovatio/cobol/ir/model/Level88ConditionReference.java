package org.shark.renovatio.cobol.ir.model;

import java.util.Locale;
import java.util.Objects;

public record Level88ConditionReference(String conditionName, SourceSpan sourceSpan)
        implements CobolCondition {

    public Level88ConditionReference {
        Objects.requireNonNull(conditionName, "conditionName");
        if (conditionName.isBlank()) {
            throw new IllegalArgumentException("conditionName must not be blank");
        }
        conditionName = conditionName.toUpperCase(Locale.ROOT);
        Objects.requireNonNull(sourceSpan, "sourceSpan");
    }
}
