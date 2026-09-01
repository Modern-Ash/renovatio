package org.shark.renovatio.api.dto;

import org.shark.renovatio.architecture.ArchitectureGraph;
import org.shark.renovatio.architecture.ArchitectureResult;
import org.shark.renovatio.architecture.ArtifactManifest;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.List;

/** Stable, target-neutral API projection of the canonical architecture result. */
public record ArchitecturePreviewDto(
        String schemaVersion,
        String requestHash,
        String profileHash,
        List<Program> programs,
        List<Module> modules,
        List<Component> components,
        List<Relation> relations,
        List<Artifact> artifacts,
        List<Diagnostic> diagnostics,
        boolean hasFallback) {

    public static ArchitecturePreviewDto from(ArchitectureResult result) {
        List<Program> programs = result.programs().stream().map(Program::from).toList();
        return new ArchitecturePreviewDto(
                result.schemaVersion(),
                result.requestHash(),
                result.programs().get(0).targetModel().profileHash(),
                programs,
                result.graph().modules().stream().map(Module::from).toList(),
                result.graph().components().stream().map(Component::from).toList(),
                result.graph().relations().stream().map(Relation::from).toList(),
                result.manifest().artifacts().stream().map(Artifact::from).toList(),
                result.diagnostics().stream().map(Diagnostic::from).toList(),
                programs.stream().anyMatch(Program::fallback));
    }

    public record Program(String programId, String moduleId,
                          MigrationProfile.ArchitectureStyle requestedStyle,
                          MigrationProfile.ArchitectureStyle effectiveStyle,
                          List<String> componentIds, List<String> artifactIds,
                          boolean fallback) {
        private static Program from(ArchitectureResult.ArchitectedProgram value) {
            return new Program(value.programId(), value.moduleId(), value.requestedStyle(), value.effectiveStyle(),
                    value.componentIds(), value.artifactIds(), value.requestedStyle() != value.effectiveStyle());
        }
    }

    public record Module(String id, String name, List<String> programIds) {
        private static Module from(ArchitectureGraph.Module value) {
            return new Module(value.id(), value.name(), value.programIds());
        }
    }

    public record Component(String id, String moduleId, String programId, String semanticNodeId,
                            ArchitectureGraph.ComponentKind kind, String name) {
        private static Component from(ArchitectureGraph.Component value) {
            return new Component(value.id(), value.moduleId(), value.programId(),
                    value.semanticNodeId().orElse(null), value.kind(), value.name());
        }
    }

    public record Relation(String id, String fromComponentId, String toComponentId,
                           ArchitectureGraph.RelationKind kind) {
        private static Relation from(ArchitectureGraph.Relation value) {
            return new Relation(value.id(), value.fromComponentId(), value.toComponentId(), value.kind());
        }
    }

    public record Artifact(String id, String path, String componentId, String moduleId, String programId,
                           MigrationProfile.Language targetLanguage, String role) {
        private static Artifact from(ArtifactManifest.Artifact value) {
            return new Artifact(value.id(), value.path(), value.componentId(), value.moduleId(), value.programId(),
                    value.targetLanguage(), value.role());
        }
    }

    public record Diagnostic(String code, String programId, String message, List<String> evidenceHashes) {
        private static Diagnostic from(ArchitectureResult.Diagnostic value) {
            return new Diagnostic(value.code(), value.programId(), value.message(), value.evidenceHashes());
        }
    }
}
