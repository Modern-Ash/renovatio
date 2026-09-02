package org.shark.renovatio.persistence.registry;

import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when no strategy is available for a given DataAccessKind + target language.
 */
public class PersistenceStrategyUnavailableException extends RuntimeException {

    private final DataAccessKind requestedKind;
    private final MigrationProfile.Language requestedTarget;
    private final List<MigrationProfile.Language> availableTargets;

    public PersistenceStrategyUnavailableException(DataAccessKind requestedKind,
                                                    MigrationProfile.Language requestedTarget,
                                                    List<MigrationProfile.Language> availableTargets) {
        super("No persistence strategy for " + requestedKind + " targeting " + requestedTarget
                + ". Available targets: " + availableTargets);
        this.requestedKind = Objects.requireNonNull(requestedKind);
        this.requestedTarget = Objects.requireNonNull(requestedTarget);
        this.availableTargets = List.copyOf(Objects.requireNonNull(availableTargets));
    }

    public DataAccessKind requestedKind() { return requestedKind; }
    public MigrationProfile.Language requestedTarget() { return requestedTarget; }
    public List<MigrationProfile.Language> availableTargets() { return availableTargets; }
}
