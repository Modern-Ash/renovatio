package org.shark.renovatio.emitter.node;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;

@FunctionalInterface
public interface NodeArtifactRenderer {
    EmittedArtifacts render(TargetModel model, MigrationProfile profile);
}
