package org.modernash.renovatio.emitter.node;

import org.modernash.renovatio.profile.MigrationProfile;
import org.modernash.renovatio.shared.emission.EmittedArtifacts;
import org.modernash.renovatio.shared.emission.TargetModel;
import org.modernash.renovatio.shared.spi.TargetEmitter;

import java.util.Objects;

public final class NodeEmitter implements TargetEmitter {
    private final NodeArtifactRenderer renderer;

    public NodeEmitter(NodeArtifactRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public boolean supports(MigrationProfile.Language target) {
        return target == MigrationProfile.Language.NODE;
    }

    @Override
    public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) {
        Objects.requireNonNull(model, "model");
        if (!model.profile().equals(profile)) {
            throw new IllegalArgumentException("emitter profile must equal target model profile");
        }
        return Objects.requireNonNull(renderer.render(model, profile), "renderer result");
    }
}
