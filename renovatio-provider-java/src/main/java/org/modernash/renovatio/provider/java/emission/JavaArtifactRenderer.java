package org.modernash.renovatio.provider.java.emission;

import org.modernash.renovatio.profile.MigrationProfile;
import org.modernash.renovatio.shared.emission.EmittedArtifacts;
import org.modernash.renovatio.shared.emission.TargetModel;

/** Java-target renderer hidden behind the public target SPI. */
@FunctionalInterface
public interface JavaArtifactRenderer {
    EmittedArtifacts render(TargetModel model, MigrationProfile profile);
}
