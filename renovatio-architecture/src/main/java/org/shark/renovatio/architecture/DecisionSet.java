package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable architectural decisions consumed by the canonical projection. */
public record DecisionSet(String schemaVersion, String profileHash,
                          MigrationProfile.ArchitectureStyle architectureStyle,
                          MigrationProfile.Language targetLanguage,
                          Map<String, String> moduleByProgram, List<String> appliedDecisionIds) {
    public static final String SCHEMA_VERSION = "1";

    public DecisionSet {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException(
                "unsupported DecisionSet schemaVersion '" + schemaVersion + "'; supported: " + SCHEMA_VERSION);
        profileHash = ArchitectureSupport.hash(profileHash, "profileHash");
        Objects.requireNonNull(architectureStyle, "architectureStyle");
        Objects.requireNonNull(targetLanguage, "targetLanguage");
        TreeMap<String, String> ordered = new TreeMap<>();
        Objects.requireNonNull(moduleByProgram, "moduleByProgram").forEach((program, module) ->
                ordered.put(ArchitectureSupport.program(program), ArchitectureSupport.moduleName(module)));
        if (ordered.isEmpty()) throw new IllegalArgumentException("moduleByProgram must not be empty");
        moduleByProgram = Map.copyOf(ordered);
        appliedDecisionIds = (appliedDecisionIds == null ? List.<String>of() : appliedDecisionIds).stream()
                .map(value -> ArchitectureSupport.text(value, "decisionId")).distinct().sorted().toList();
    }

    public String canonicalHash() {
        return ArchitectureSupport.sha256(schemaVersion + "\n" + profileHash + "\n" + architectureStyle
                + "\n" + targetLanguage + "\n" + moduleByProgram + "\n" + appliedDecisionIds);
    }
}
