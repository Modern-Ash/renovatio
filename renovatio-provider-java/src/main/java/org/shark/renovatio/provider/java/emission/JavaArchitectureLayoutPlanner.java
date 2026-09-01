package org.shark.renovatio.provider.java.emission;

import org.shark.renovatio.architecture.ArchitectureGraph;
import org.shark.renovatio.architecture.ArtifactLayoutPlanner;
import org.shark.renovatio.profile.MigrationProfile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Canonical Java layout shared by preview and Java emission orchestration. */
public final class JavaArchitectureLayoutPlanner implements ArtifactLayoutPlanner {
    @Override
    public MigrationProfile.Language targetLanguage() {
        return MigrationProfile.Language.JAVA;
    }

    @Override
    public List<PlannedArtifact> plan(LayoutContext context) {
        String classBase = classBase(context.program().sourceProvenance().sourcePath());
        String model = component(context, ArchitectureGraph.ComponentKind.ENTITY,
                ArchitectureGraph.ComponentKind.VALUE, ArchitectureGraph.ComponentKind.SERVICE,
                ArchitectureGraph.ComponentKind.USE_CASE);
        String contract = component(context, ArchitectureGraph.ComponentKind.INBOUND_PORT,
                ArchitectureGraph.ComponentKind.SERVICE, ArchitectureGraph.ComponentKind.USE_CASE);
        String implementation = component(context, ArchitectureGraph.ComponentKind.USE_CASE,
                ArchitectureGraph.ComponentKind.SERVICE, ArchitectureGraph.ComponentKind.INBOUND_PORT);
        String prefix = context.effectiveStyle() == MigrationProfile.ArchitectureStyle.HEXAGONAL
                ? "modules/" + context.moduleName() + "/" : "";
        List<PlannedArtifact> result = new ArrayList<>();
        result.add(new PlannedArtifact(prefix + (prefix.isEmpty() ? "" : "domain/model/")
                + classBase + "DTO.java", model, "data-transfer-object"));
        result.add(new PlannedArtifact(prefix + (prefix.isEmpty() ? "" : "application/port/in/")
                + classBase + "Service.java", contract, "service-contract"));
        result.add(new PlannedArtifact(prefix + (prefix.isEmpty() ? "" : "application/service/")
                + classBase + "ServiceImpl.java", implementation, "service-implementation"));
        return List.copyOf(result);
    }

    private static String component(LayoutContext context, ArchitectureGraph.ComponentKind... preferred) {
        for (ArchitectureGraph.ComponentKind kind : preferred) {
            var match = context.components().stream().filter(value -> value.kind() == kind)
                    .min(Comparator.comparing(ArchitectureGraph.Component::id));
            if (match.isPresent()) return match.orElseThrow().id();
        }
        return context.components().stream().min(Comparator.comparing(ArchitectureGraph.Component::id))
                .orElseThrow(() -> new IllegalArgumentException("Java layout requires an architecture component")).id();
    }

    private static String classBase(String sourcePath) {
        String fileName = Path.of(sourcePath).getFileName().toString();
        String withoutExtension = fileName.replaceFirst("(?i)\\.(cob|cobol|cbl|cpy)$", "");
        String[] parts = withoutExtension.replaceAll("[^a-zA-Z0-9]", " ").trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || common(part)) continue;
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) result.append(part.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        String candidate = result.isEmpty() ? "CobolProgram" : result.toString().replaceAll("[^A-Za-z0-9]", "");
        if (candidate.isEmpty()) return "CobolProgram";
        return Character.isLetter(candidate.charAt(0)) ? candidate : "Cobol" + candidate;
    }

    private static boolean common(String value) {
        return value.equalsIgnoreCase("cob") || value.equalsIgnoreCase("cobol")
                || value.equalsIgnoreCase("cbl") || value.equalsIgnoreCase("cpy")
                || value.equalsIgnoreCase("program") || value.equalsIgnoreCase("file");
    }
}
