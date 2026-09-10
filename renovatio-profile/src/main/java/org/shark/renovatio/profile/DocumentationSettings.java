package org.shark.renovatio.profile;

import java.util.Map;
import java.util.Objects;

/** Typed access to deterministic explanatory source-documentation settings. */
public final class DocumentationSettings {
    public static final String EXTENSION_KEY = "documentation.enabled";

    private DocumentationSettings() { }

    public static boolean enabled(MigrationProfile profile) {
        Objects.requireNonNull(profile, "profile");
        Map<String, Object> extensions = profile.extensions();
        if (extensions == null || !extensions.containsKey(EXTENSION_KEY)) return false;
        Object configured = extensions.get(EXTENSION_KEY);
        if (configured instanceof Boolean enabled) return enabled;
        throw new IllegalArgumentException(EXTENSION_KEY + " must be a boolean");
    }
}
