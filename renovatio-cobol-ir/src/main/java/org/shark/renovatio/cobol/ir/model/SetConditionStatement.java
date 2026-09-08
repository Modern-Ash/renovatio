package org.shark.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** COBOL {@code SET condition-name TO TRUE|FALSE} for level-88 condition names. */
public record SetConditionStatement(List<String> conditionNames, boolean value,
                                    String sourceText) implements CobolStatement {

    public SetConditionStatement {
        conditionNames = List.copyOf(Objects.requireNonNull(conditionNames, "conditionNames").stream()
                .map(name -> Objects.requireNonNull(name, "conditionName").strip().toUpperCase(Locale.ROOT))
                .toList());
        if (conditionNames.isEmpty() || conditionNames.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("conditionNames must not be empty");
        }
        sourceText = Objects.requireNonNull(sourceText, "sourceText").strip();
        if (sourceText.isEmpty()) {
            throw new IllegalArgumentException("sourceText must not be empty");
        }
    }
}
