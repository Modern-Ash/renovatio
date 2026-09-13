package org.modernash.renovatio.emitter.node;

import org.modernash.renovatio.profile.MigrationProfile;
import org.modernash.renovatio.shared.emission.EmittedArtifacts;
import org.modernash.renovatio.shared.emission.TargetModel;

@FunctionalInterface
public interface NodeArtifactRenderer {
    EmittedArtifacts render(TargetModel model, MigrationProfile profile);
}
