package org.shark.renovatio.provider.java.emission;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.spi.TargetEmitter;

import java.util.Objects;

/** The sole F2 Java target adapter. */
public final class JavaEmitter implements TargetEmitter {
    private final JavaArtifactRenderer renderer;

    public JavaEmitter(JavaArtifactRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public boolean supports(MigrationProfile.Language target) {
        return target == MigrationProfile.Language.JAVA;
    }

    @Override
    public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) {
        Objects.requireNonNull(model, "model");
        if (!model.profile().equals(profile)) throw new IllegalArgumentException("emitter profile must equal target model profile");
        return Objects.requireNonNull(renderer.render(model, profile), "renderer result");
    }
}
