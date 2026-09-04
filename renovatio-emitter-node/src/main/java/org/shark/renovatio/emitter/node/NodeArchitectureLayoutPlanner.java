package org.shark.renovatio.emitter.node;

import org.shark.renovatio.architecture.ArchitectureGraph;
import org.shark.renovatio.architecture.ArtifactLayoutPlanner;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NodeArchitectureLayoutPlanner implements ArtifactLayoutPlanner {
    @Override
    public MigrationProfile.Language targetLanguage() {
        return MigrationProfile.Language.NODE;
    }

    @Override
    public List<PlannedArtifact> plan(LayoutContext context) {
        Map<String, PlannedArtifact> artifacts = new LinkedHashMap<>();
        String moduleDir = moduleDir(context.moduleName());
        String programId = context.program().programId();

        for (ArchitectureGraph.Component component : context.components()) {
            String componentId = component.id();
            String role = component.kind().name().toLowerCase(Locale.ROOT);
            String path = switch (component.kind()) {
                case SERVICE, USE_CASE -> moduleDir + "/domain/" + programId.toLowerCase(Locale.ROOT) + ".service.ts";
                case ENTITY -> moduleDir + "/domain/" + programId.toLowerCase(Locale.ROOT) + ".entity.ts";
                case INBOUND_PORT -> moduleDir + "/domain/" + programId.toLowerCase(Locale.ROOT) + ".repository.ts";
                case OUTBOUND_PORT, ADAPTER -> moduleDir + "/api/" + programId.toLowerCase(Locale.ROOT) + ".controller.ts";
                default -> moduleDir + "/domain/" + programId.toLowerCase(Locale.ROOT) + "." + role + ".ts";
            };
            artifacts.putIfAbsent(path, new PlannedArtifact(path, componentId, role));
        }

        return List.copyOf(artifacts.values());
    }

    private String moduleDir(String moduleName) {
        return "src/" + moduleName.toLowerCase(Locale.ROOT).replace(' ', '-');
    }
}
