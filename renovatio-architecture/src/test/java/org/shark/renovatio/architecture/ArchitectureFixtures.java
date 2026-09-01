package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ArchitectureFixtures {
    private ArchitectureFixtures() { }

    static SemanticProgram program(String id, String path, char hash) {
        SourceSpan span = new SourceSpan(path, 1, 1, 2, 9);
        return new SemanticProgram("1", SemanticProgram.Header.create(id,
                SemanticProgram.NodeKind.PROGRAM, "program", span), id,
                new SourceProvenance(path, String.valueOf(hash).repeat(64), "COBOL", Optional.empty(), List.of()),
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
    }

    static MigrationProfiles.EffectiveProfile effective(MigrationProfile.ArchitectureStyle style,
                                                         MigrationProfile.ModuleGrouping grouping) {
        MigrationProfile overlay = new MigrationProfile("1", Map.of(), null,
                new MigrationProfile.Architecture(style, grouping), null, null, null, null);
        return MigrationProfiles.effective(overlay, Map.of(), Map.of(), List.of());
    }
}
