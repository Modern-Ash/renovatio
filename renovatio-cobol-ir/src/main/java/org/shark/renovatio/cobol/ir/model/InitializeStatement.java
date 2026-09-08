package org.shark.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** COBOL {@code INITIALIZE} over one or more elementary data names. */
public record InitializeStatement(List<String> targets, String sourceText) implements CobolStatement {

    public InitializeStatement {
        targets = List.copyOf(Objects.requireNonNull(targets, "targets").stream()
                .map(target -> Objects.requireNonNull(target, "target").strip().toUpperCase(Locale.ROOT))
                .toList());
        if (targets.isEmpty() || targets.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("targets must not be empty");
        }
        sourceText = Objects.requireNonNull(sourceText, "sourceText").strip();
        if (sourceText.isEmpty()) {
            throw new IllegalArgumentException("sourceText must not be empty");
        }
    }
}
