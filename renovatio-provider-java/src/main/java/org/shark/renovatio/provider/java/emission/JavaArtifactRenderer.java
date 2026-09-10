package org.shark.renovatio.provider.java.emission;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;

/** Java-target renderer hidden behind the public target SPI. */
@FunctionalInterface
public interface JavaArtifactRenderer {
    EmittedArtifacts render(TargetModel model, MigrationProfile profile);
}
