package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.EmittedArtifact;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Canonical pre-emission artifact layout. */
public record ArtifactManifest(List<Artifact> artifacts) {
    public ArtifactManifest {
        artifacts = (artifacts == null ? List.<Artifact>of() : artifacts).stream()
                .peek(Objects::requireNonNull).sorted(java.util.Comparator.comparing(Artifact::path)).toList();
        unique(artifacts.stream().map(Artifact::id).toList(), "artifact id");
        unique(artifacts.stream().map(Artifact::path).toList(), "artifact path");
    }

    public record Artifact(String id, String path, String componentId, String moduleId, String programId,
                           MigrationProfile.Language targetLanguage, String role) {
        public Artifact {
            id = ArchitectureSupport.hash(id, "artifactId");
            path = new EmittedArtifact(path, new byte[0]).path();
            componentId = ArchitectureSupport.hash(componentId, "componentId");
            moduleId = ArchitectureSupport.hash(moduleId, "moduleId");
            programId = ArchitectureSupport.program(programId);
            Objects.requireNonNull(targetLanguage, "targetLanguage");
            role = ArchitectureSupport.text(role, "artifactRole");
        }
    }

    private static void unique(List<String> values, String name) {
        if (new HashSet<>(values).size() != values.size()) throw new IllegalArgumentException("duplicate " + name);
    }
}
