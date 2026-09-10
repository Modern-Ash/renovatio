package org.shark.renovatio.profile;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable, content-addressed reusable migration-profile overlay. */
public record MigrationProfileTemplate(
        String schemaVersion,
        String name,
        String version,
        String description,
        MigrationProfile profile,
        String contentHash,
        Instant createdAt) {

    public static final String SCHEMA_VERSION = "1";

    public MigrationProfileTemplate {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("schemaVersion must equal 1");
        ReusableAssetIdentifier.require(name, "name");
        ReusableAssetIdentifier.require(version, "version");
        if (description != null && description.length() > 500) throw new IllegalArgumentException("description is too long");
        if (!MigrationProfiles.validateOverlay(profile).isEmpty()) {
            throw new MigrationProfiles.ProfileValidationException(MigrationProfiles.validateOverlay(profile));
        }
        if (contentHash == null || !contentHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("contentHash must be lowercase SHA-256");
        }
        if (createdAt == null) throw new IllegalArgumentException("createdAt is required");
    }

    public static MigrationProfileTemplate create(String name, String version, String description,
                                                   MigrationProfile profile, Instant createdAt) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("schemaVersion", SCHEMA_VERSION);
        content.put("name", name);
        content.put("version", version);
        content.put("description", description);
        content.put("profile", profile);
        return new MigrationProfileTemplate(SCHEMA_VERSION, name, version, description, profile,
                MigrationProfiles.sha256(MigrationProfiles.canonical(content)), createdAt);
    }

    public TemplateReference reference() { return new TemplateReference(name, version); }
}
