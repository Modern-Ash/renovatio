package org.shark.renovatio.shared.emission;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable one-to-one envelope passed from semantic analysis to a target emitter. */
public record TargetModel(SemanticProgram semanticProgram, MigrationProfile profile,
                          Map<String, String> resolvedDecisions, List<String> appliedDecisionIds,
                          String profileHash, SourceProvenance sourceProvenance,
                          TargetStructure targetStructure) {
    public TargetModel {
        Objects.requireNonNull(semanticProgram, "semanticProgram");
        Objects.requireNonNull(profile, "profile");
        if (profile.target() == null || profile.target().language() == null) {
            throw new IllegalArgumentException("effective profile target is required");
        }
        TreeMap<String, String> decisions = new TreeMap<>(Objects.requireNonNull(resolvedDecisions,
                "resolvedDecisions"));
        if (decisions.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank()
                || entry.getValue() == null || entry.getValue().isBlank())) {
            throw new IllegalArgumentException("resolved decisions must contain non-blank keys and values");
        }
        resolvedDecisions = Collections.unmodifiableMap(new LinkedHashMap<>(decisions));
        appliedDecisionIds = Objects.requireNonNull(appliedDecisionIds, "appliedDecisionIds").stream()
                .map(TargetModel::hash).distinct().sorted().toList();
        profileHash = hash(profileHash);
        sourceProvenance = Objects.requireNonNull(sourceProvenance, "sourceProvenance");
        if (!sourceProvenance.equals(semanticProgram.sourceProvenance())) {
            throw new IllegalArgumentException("target and semantic provenance must match");
        }
        targetStructure = Objects.requireNonNull(targetStructure, "targetStructure");
    }

    /** Backward-compatible F2 constructor producing an identity architecture slice. */
    public TargetModel(SemanticProgram semanticProgram, MigrationProfile profile,
                       Map<String, String> resolvedDecisions, List<String> appliedDecisionIds,
                       String profileHash, SourceProvenance sourceProvenance) {
        this(semanticProgram, profile, resolvedDecisions, appliedDecisionIds, profileHash, sourceProvenance,
                TargetStructure.identity(semanticProgram, profileHash,
                        Objects.requireNonNull(profile, "profile").architecture().style()));
    }

    public static TargetModel from(SemanticProgram program, MigrationProfiles.EffectiveProfile effective) {
        Objects.requireNonNull(effective, "effective");
        return new TargetModel(program, effective.profile(), effective.resolvedDecisions(),
                effective.appliedDecisionIds(), effective.profileHash(), program.sourceProvenance(),
                TargetStructure.identity(program, effective.profileHash(), effective.profile().architecture().style()));
    }

    public MigrationProfile.Language targetLanguage() { return profile.target().language(); }

    private static String hash(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("hash must be a lowercase SHA-256");
        }
        return value;
    }
}
