package org.shark.renovatio.persistence.strategy;

import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.List;
import java.util.Objects;

/**
 * Immutable persistence artifacts emitted by a strategy for one classified data access.
 */
public record PersistenceArtifacts(
    String entityId,
    String entitySource,
    String repositoryId,
    String repositorySource,
    String configSnippet,
    List<String> diagnostics
) {
    public PersistenceArtifacts {
        entityId = Objects.requireNonNull(entityId, "entityId");
        entitySource = Objects.requireNonNull(entitySource, "entitySource");
        repositoryId = Objects.requireNonNull(repositoryId, "repositoryId");
        repositorySource = Objects.requireNonNull(repositorySource, "repositorySource");
        configSnippet = configSnippet == null ? "" : configSnippet;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
