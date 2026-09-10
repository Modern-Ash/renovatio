package org.shark.renovatio.core.service;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.spi.TargetEmitter;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

/** @deprecated Use the application-neutral shared emission registry. */
@Deprecated(forRemoval = true)
@Service
public class TargetEmitterRegistry extends org.shark.renovatio.shared.emission.TargetEmitterRegistry {
    public TargetEmitterRegistry(Collection<TargetEmitter> candidates) {
        super(validateCompatibilityDuplicates(candidates));
    }

    @Override
    public TargetEmitter resolve(MigrationProfile.Language target) {
        try {
            return super.resolve(target);
        } catch (org.shark.renovatio.shared.emission.TargetEmitterRegistry.TargetEmitterUnavailableException error) {
            throw new TargetEmitterUnavailableException(error.requestedTarget(), error.availableTargets());
        }
    }

    @Override
    public EmittedArtifacts emit(TargetModel model, TargetEmitter requestAdapter) {
        try {
            return super.emit(model, requestAdapter);
        } catch (org.shark.renovatio.shared.emission.TargetEmitterRegistry.TargetEmitterUnavailableException error) {
            throw new TargetEmitterUnavailableException(error.requestedTarget(), error.availableTargets());
        } catch (org.shark.renovatio.shared.emission.TargetEmitterRegistry.DuplicateTargetEmitterException error) {
            throw new DuplicateTargetEmitterException(error.target(), error.emitterTypes());
        }
    }

    @Override
    public EmittedArtifacts emit(TargetModel model,
                                 BiFunction<TargetModel, MigrationProfile, EmittedArtifacts> renderer) {
        try {
            return super.emit(model, renderer);
        } catch (org.shark.renovatio.shared.emission.TargetEmitterRegistry.TargetEmitterUnavailableException error) {
            throw new TargetEmitterUnavailableException(error.requestedTarget(), error.availableTargets());
        } catch (org.shark.renovatio.shared.emission.TargetEmitterRegistry.DuplicateTargetEmitterException error) {
            throw new DuplicateTargetEmitterException(error.target(), error.emitterTypes());
        }
    }

    private static Collection<TargetEmitter> validateCompatibilityDuplicates(Collection<TargetEmitter> candidates) {
        List<TargetEmitter> ordered = (candidates == null ? List.<TargetEmitter>of() : candidates).stream()
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(value -> value.getClass().getName()))
                .toList();
        for (MigrationProfile.Language target : MigrationProfile.Language.values()) {
            List<TargetEmitter> supported = ordered.stream().filter(emitter -> emitter.supports(target)).toList();
            if (supported.size() > 1) throw new DuplicateTargetEmitterException(target,
                    supported.stream().map(value -> value.getClass().getName()).toList());
        }
        return ordered;
    }

    /** @deprecated Compatibility binary for the former Core registry API. */
    @Deprecated(forRemoval = true)
    public static final class TargetEmitterUnavailableException extends IllegalStateException {
        public static final String CODE = "TARGET_EMITTER_UNAVAILABLE";
        private final MigrationProfile.Language requestedTarget;
        private final List<MigrationProfile.Language> availableTargets;

        private TargetEmitterUnavailableException(MigrationProfile.Language requestedTarget,
                                                   Collection<MigrationProfile.Language> availableTargets) {
            super(CODE + ": requested=" + requestedTarget + ", available=" + availableTargets);
            this.requestedTarget = requestedTarget;
            this.availableTargets = availableTargets.stream().sorted(Comparator.comparing(Enum::name)).toList();
        }

        public String code() { return CODE; }
        public MigrationProfile.Language requestedTarget() { return requestedTarget; }
        public List<MigrationProfile.Language> availableTargets() { return availableTargets; }
    }

    /** @deprecated Compatibility binary for the former Core registry API. */
    @Deprecated(forRemoval = true)
    public static final class DuplicateTargetEmitterException extends IllegalStateException {
        private final MigrationProfile.Language target;
        private final List<String> emitterTypes;

        private DuplicateTargetEmitterException(MigrationProfile.Language target, List<String> emitterTypes) {
            super("Duplicate target emitters for " + target + ": " + emitterTypes);
            this.target = target;
            this.emitterTypes = emitterTypes.stream().sorted().toList();
        }

        public MigrationProfile.Language target() { return target; }
        public List<String> emitterTypes() { return emitterTypes; }
    }
}
