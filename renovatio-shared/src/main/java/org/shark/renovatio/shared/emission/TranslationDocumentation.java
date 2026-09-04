package org.shark.renovatio.shared.emission;

import java.text.Normalizer;
import java.util.Objects;
import java.util.stream.Collectors;

/** Deterministic, comment-safe explanatory documentation derived from a target envelope. */
public final class TranslationDocumentation {
    private TranslationDocumentation() { }

    public static String javadoc(TargetModel model) {
        return block(model);
    }

    public static String tsdoc(TargetModel model) {
        return block(model);
    }

    private static String block(TargetModel model) {
        Objects.requireNonNull(model, "model");
        String decisions = model.resolvedDecisions().isEmpty() ? "none"
                : model.resolvedDecisions().entrySet().stream()
                .map(entry -> safe(entry.getKey()) + "=" + safe(entry.getValue()))
                .collect(Collectors.joining("; "));
        String references = model.appliedDecisionIds().isEmpty() ? "none"
                : model.appliedDecisionIds().stream().map(TranslationDocumentation::safe)
                .collect(Collectors.joining("; "));
        return """
                /**
                 * Migrated from COBOL program %s (%s).
                 * Effective decisions: %s.
                 * DecisionPoint references: %s.
                 */
                """.formatted(safe(model.semanticProgram().programId()),
                safe(model.sourceProvenance().sourcePath()), decisions, references);
    }

    private static String safe(String value) {
        String normalized = Normalizer.normalize(Objects.requireNonNull(value, "value"),
                Normalizer.Form.NFC);
        StringBuilder result = new StringBuilder(normalized.length());
        boolean previousSpace = false;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            boolean space = Character.isWhitespace(current) || Character.isISOControl(current);
            if (space) {
                if (!previousSpace) result.append(' ');
            } else {
                result.append(current);
            }
            previousSpace = space;
        }
        return result.toString().trim().replace("*/", "* /");
    }
}
