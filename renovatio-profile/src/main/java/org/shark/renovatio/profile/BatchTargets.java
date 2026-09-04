package org.shark.renovatio.profile;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Typed access to the backwards-compatible {@code batch.target} profile extension. */
public final class BatchTargets {
    public static final String EXTENSION_KEY = "batch.target";

    private BatchTargets() { }

    public static MigrationProfile.BatchTarget resolve(MigrationProfile profile) {
        Objects.requireNonNull(profile, "profile");
        Map<String, Object> extensions = profile.extensions();
        Object configured = extensions == null ? null : extensions.get(EXTENSION_KEY);
        if (configured == null) {
            if (profile.target() != null && profile.target().language() == MigrationProfile.Language.JAVA) {
                return MigrationProfile.BatchTarget.SPRING_BATCH;
            }
            throw new IllegalArgumentException("batch.target is required for non-Java targets");
        }
        try {
            return MigrationProfile.BatchTarget.valueOf(configured.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported batch.target: " + configured, exception);
        }
    }

    public static boolean isActive(MigrationProfile profile) {
        return resolve(profile) == MigrationProfile.BatchTarget.SPRING_BATCH;
    }
}
