package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import java.util.List;

/** Layered MVC projection: program entrypoints become controllers and operations become services. */
public final class LayeredMvcArchitectureProfile implements ArchitectureProfile {
    private final TransactionScriptArchitectureProfile base = new TransactionScriptArchitectureProfile();
    @Override public MigrationProfile.ArchitectureStyle style() { return MigrationProfile.ArchitectureStyle.LAYERED_MVC; }
    @Override public ProgramResult transform(ProgramContext context) {
        ProgramResult result = base.transform(context);
        List<ArchitectureGraph.Component> components = result.components().stream().map(component -> {
            ArchitectureGraph.ComponentKind kind = switch (component.kind()) {
                case SERVICE -> ArchitectureGraph.ComponentKind.INBOUND_PORT;
                case USE_CASE -> ArchitectureGraph.ComponentKind.SERVICE;
                default -> component.kind();
            };
            return new ArchitectureGraph.Component(component.id(), component.moduleId(), component.programId(), component.semanticNodeId(), kind, component.name());
        }).toList();
        return new ProgramResult(style(), components, result.relations(), result.diagnostics());
    }
}
