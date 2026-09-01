package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.TargetModel;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Fully validated result shared by preview and emission orchestration. */
public record ArchitectureResult(String schemaVersion, String requestHash,
                                 List<ArchitectedProgram> programs,
                                 ArchitectureGraph graph, ArtifactManifest manifest,
                                 List<Diagnostic> diagnostics) {
    public static final String SCHEMA_VERSION = "1";

    public ArchitectureResult {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("unsupported schemaVersion");
        requestHash = ArchitectureSupport.hash(requestHash, "requestHash");
        programs = (programs == null ? List.<ArchitectedProgram>of() : programs).stream()
                .peek(Objects::requireNonNull).sorted(java.util.Comparator.comparing(ArchitectedProgram::programId))
                .toList();
        if (programs.isEmpty()) throw new IllegalArgumentException("program results must not be empty");
        unique(programs.stream().map(ArchitectedProgram::programId).toList(), "program result");
        graph = Objects.requireNonNull(graph, "graph");
        manifest = Objects.requireNonNull(manifest, "manifest");
        diagnostics = (diagnostics == null ? List.<Diagnostic>of() : diagnostics).stream()
                .peek(Objects::requireNonNull).sorted(java.util.Comparator.comparing(Diagnostic::code)
                        .thenComparing(Diagnostic::programId)).toList();
        Set<String> programIds = new HashSet<>(programs.stream().map(ArchitectedProgram::programId).toList());
        graph.modules().forEach(value -> {
            if (!programIds.containsAll(value.programIds())) throw new IllegalArgumentException("module references unknown program");
        });
        graph.components().forEach(value -> {
            if (!programIds.contains(value.programId())) throw new IllegalArgumentException("component references unknown program");
        });
    }

    public record ArchitectedProgram(String programId, String moduleId,
                                     MigrationProfile.ArchitectureStyle requestedStyle,
                                     MigrationProfile.ArchitectureStyle effectiveStyle,
                                     TargetModel targetModel, List<String> componentIds,
                                     List<String> artifactIds) {
        public ArchitectedProgram {
            programId = ArchitectureSupport.program(programId);
            moduleId = ArchitectureSupport.hash(moduleId, "moduleId");
            Objects.requireNonNull(requestedStyle, "requestedStyle");
            Objects.requireNonNull(effectiveStyle, "effectiveStyle");
            targetModel = Objects.requireNonNull(targetModel, "targetModel");
            if (!programId.equals(targetModel.semanticProgram().programId())) {
                throw new IllegalArgumentException("target model program mismatch");
            }
            componentIds = hashes(componentIds, "componentId");
            artifactIds = hashes(artifactIds, "artifactId");
        }
    }

    public record Diagnostic(String code, String programId, String message, List<String> evidenceHashes) {
        public Diagnostic {
            code = ArchitectureSupport.text(code, "diagnosticCode");
            programId = ArchitectureSupport.program(programId);
            message = ArchitectureSupport.text(message, "diagnosticMessage");
            evidenceHashes = hashes(evidenceHashes, "evidenceHash");
        }
    }

    private static List<String> hashes(List<String> values, String name) {
        return (values == null ? List.<String>of() : values).stream()
                .map(value -> ArchitectureSupport.hash(value, name)).distinct().sorted().toList();
    }

    private static void unique(List<String> values, String name) {
        if (new HashSet<>(values).size() != values.size()) throw new IllegalArgumentException("duplicate " + name);
    }
}
