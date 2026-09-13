package org.modernash.renovatio.persistence.strategy;

import org.modernash.renovatio.persistence.classifier.DataAccessClassification;
import org.modernash.renovatio.profile.MigrationProfile;
import org.modernash.renovatio.profile.MigrationProfiles;

/**
 * Pluggable persistence strategy SPI.
 * Each implementation handles one or more DataAccessKind values for a target language.
 */
public interface PersistenceStrategy {

    /**
     * Whether this strategy can handle the given classification + target language.
     * Pure and side-effect-free.
     */
    boolean supports(DataAccessClassification classification, MigrationProfile.Language target);

    /**
     * Emit persistence artifacts for the classified data access.
     * Returns entity source, repository source, and configuration snippet.
     */
    PersistenceArtifacts emit(DataAccessClassification classification,
                              MigrationProfiles.EffectiveProfile profile);
}
