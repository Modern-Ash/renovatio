package org.shark.renovatio.decisions;

import org.shark.renovatio.profile.MigrationProfiles;

import java.text.Normalizer;
import java.util.Locale;

/** Stable semantic-coordinate identity projector for DecisionPoint v1. */
public final class DecisionIdentity {
    private DecisionIdentity() { }

    public static String id(DecisionPoint.Category category, String decisionKey,
                            DecisionPoint.Location location) {
        String projection = String.join("\n", "decision-point.v1", category.name(),
                normalize(decisionKey), normalizeProgram(location.programId()),
                normalize(location.nodeKind()).toUpperCase(Locale.ROOT), normalize(location.nodeId()));
        return MigrationProfiles.sha256(projection);
    }

    private static String normalizeProgram(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("identity value is required");
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
    }
}
