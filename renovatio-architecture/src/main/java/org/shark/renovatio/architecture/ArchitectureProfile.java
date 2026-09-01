package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.List;
import java.util.Objects;

/** Pure per-program architecture transformation selected by profile style. */
public interface ArchitectureProfile {
    MigrationProfile.ArchitectureStyle style();

    ProgramResult transform(ProgramContext context);

    record ProgramContext(String requestHash, String moduleId, SemanticProgram program,
                          MigrationProfiles.EffectiveProfile effectiveProfile) {
        public ProgramContext {
            requestHash = ArchitectureSupport.hash(requestHash, "requestHash");
            moduleId = ArchitectureSupport.hash(moduleId, "moduleId");
            Objects.requireNonNull(program, "program");
            Objects.requireNonNull(effectiveProfile, "effectiveProfile");
        }
    }

    record ProgramResult(MigrationProfile.ArchitectureStyle effectiveStyle,
                         List<ArchitectureGraph.Component> components,
                         List<ArchitectureGraph.Relation> relations,
                         List<ArchitectureResult.Diagnostic> diagnostics) {
        public ProgramResult {
            Objects.requireNonNull(effectiveStyle, "effectiveStyle");
            components = components == null ? List.of() : List.copyOf(components);
            relations = relations == null ? List.of() : List.copyOf(relations);
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }
}
