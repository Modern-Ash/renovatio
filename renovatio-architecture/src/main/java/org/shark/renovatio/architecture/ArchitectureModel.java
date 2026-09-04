package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Stable, target-neutral architecture projection consumed by layout and emitters. */
public record ArchitectureModel(String schemaVersion, String domainModelHash,
                                MigrationProfile.ArchitectureStyle requestedStyle,
                                MigrationProfile.ArchitectureStyle effectiveStyle,
                                List<Component> components, List<Relation> relations,
                                List<String> diagnostics) {
    public static final String SCHEMA_VERSION = "1";

    public ArchitectureModel {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("unsupported schemaVersion");
        Objects.requireNonNull(domainModelHash, "domainModelHash");
        Objects.requireNonNull(requestedStyle, "requestedStyle");
        Objects.requireNonNull(effectiveStyle, "effectiveStyle");
        components = components == null ? List.of() : components.stream().sorted(Comparator.comparing(Component::id)).toList();
        relations = relations == null ? List.of() : relations.stream().sorted(Comparator.comparing(Relation::id)).toList();
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public enum Kind { MODULE, DOMAIN, SERVICE, CONTROLLER, REPOSITORY, PORT, ADAPTER }
    public record Component(String id, Kind kind, String name, String domainNodeId) {
        public Component {
            if (id == null || id.isBlank() || name == null || name.isBlank()) throw new IllegalArgumentException("component identity required");
            Objects.requireNonNull(kind, "kind");
            domainNodeId = domainNodeId == null ? "" : domainNodeId;
        }
    }
    public record Relation(String id, String fromId, String toId, String kind) {
        public Relation {
            if (id == null || id.isBlank() || fromId == null || fromId.isBlank() || toId == null || toId.isBlank())
                throw new IllegalArgumentException("relation identity required");
            kind = kind == null ? "USES" : kind;
        }
    }
}
