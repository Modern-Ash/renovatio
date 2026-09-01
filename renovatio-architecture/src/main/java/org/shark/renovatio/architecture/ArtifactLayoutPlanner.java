package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.List;
import java.util.Objects;

/** Target adapter boundary for producing a canonical pre-emission path plan. */
public interface ArtifactLayoutPlanner {
    MigrationProfile.Language targetLanguage();

    List<PlannedArtifact> plan(LayoutContext context);

    record LayoutContext(String requestHash, String moduleId, String moduleName,
                         SemanticProgram program, MigrationProfile.ArchitectureStyle effectiveStyle,
                         List<ArchitectureGraph.Component> components) {
        public LayoutContext {
            requestHash = ArchitectureSupport.hash(requestHash, "requestHash");
            moduleId = ArchitectureSupport.hash(moduleId, "moduleId");
            moduleName = ArchitectureSupport.moduleName(moduleName);
            Objects.requireNonNull(program, "program");
            Objects.requireNonNull(effectiveStyle, "effectiveStyle");
            components = List.copyOf(Objects.requireNonNull(components, "components"));
            for (ArchitectureGraph.Component component : components) {
                if (!moduleId.equals(component.moduleId())
                        || !program.programId().equals(component.programId())) {
                    throw new IllegalArgumentException("layout components must belong to the program module");
                }
            }
        }
    }

    record PlannedArtifact(String path, String componentId, String role) {
        public PlannedArtifact {
            path = new org.shark.renovatio.shared.emission.EmittedArtifact(path, new byte[0]).path();
            componentId = ArchitectureSupport.hash(componentId, "componentId");
            role = ArchitectureSupport.text(role, "artifactRole");
        }
    }
}
