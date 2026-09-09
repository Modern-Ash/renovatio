package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Collections;
import java.util.LinkedHashMap;

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
        moduleByProgram = Collections.unmodifiableMap(new LinkedHashMap<>(ordered));
        appliedDecisionIds = (appliedDecisionIds == null ? List.<String>of() : appliedDecisionIds).stream()
                .map(value -> ArchitectureSupport.text(value, "decisionId")).distinct().sorted().toList();
    }

    public String canonicalHash() {
        StringBuilder canonical = new StringBuilder(schemaVersion).append('\n').append(profileHash).append('\n')
                .append(architectureStyle).append('\n').append(targetLanguage).append('\n');
        moduleByProgram.forEach((program, module) -> canonical.append(program).append('=').append(module).append('\n'));
        appliedDecisionIds.forEach(value -> canonical.append("decision=").append(value).append('\n'));
        return ArchitectureSupport.sha256(canonical.toString());
    }
}
