package org.modernash.renovatio.shared.spi;

import org.modernash.renovatio.profile.MigrationProfile;
import org.modernash.renovatio.shared.emission.EmittedArtifacts;
import org.modernash.renovatio.shared.emission.TargetModel;

/** Service-provider contract for target artifact emission. */
public interface TargetEmitter {
    boolean supports(MigrationProfile.Language target);
    EmittedArtifacts emit(TargetModel model, MigrationProfile profile);
}
