package org.shark.renovatio.shared.spi;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;

/** Service-provider contract for target artifact emission. */
public interface TargetEmitter {
    boolean supports(MigrationProfile.Language target);
    EmittedArtifacts emit(TargetModel model, MigrationProfile profile);
}
