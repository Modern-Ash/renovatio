package org.shark.renovatio.core.service;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.spi.TargetEmitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Fail-closed deterministic target-emitter registry. */
@Service
public class TargetEmitterRegistry {
    private final Map<MigrationProfile.Language, TargetEmitter> emitters;

    public TargetEmitterRegistry(Collection<TargetEmitter> candidates) {
        List<TargetEmitter> ordered = (candidates == null ? List.<TargetEmitter>of() : candidates).stream()
                .filter(Objects::nonNull).sorted(Comparator.comparing(value -> value.getClass().getName())).toList();
        EnumMap<MigrationProfile.Language, TargetEmitter> indexed = new EnumMap<>(MigrationProfile.Language.class);
        for (MigrationProfile.Language target : MigrationProfile.Language.values()) {
            List<TargetEmitter> supported = ordered.stream().filter(emitter -> emitter.supports(target)).toList();
            if (supported.size() > 1) throw new DuplicateTargetEmitterException(target, supported);
            if (supported.size() == 1) indexed.put(target, supported.get(0));
        }
        this.emitters = Map.copyOf(indexed);
    }

    public TargetEmitter resolve(MigrationProfile.Language target) {
        TargetEmitter emitter = emitters.get(Objects.requireNonNull(target, "target"));
        if (emitter == null) throw new TargetEmitterUnavailableException(target, emitters.keySet());
        return emitter;
    }

    public EmittedArtifacts emit(TargetModel model) {
        Objects.requireNonNull(model, "model");
        return resolve(model.targetLanguage()).emit(model, model.profile());
    }

    /**
     * Emits through the application registry while supplementing it with one request-bound adapter.
     * The adapter participates in availability and duplicate checks without hiding registered targets.
     */
    public EmittedArtifacts emit(TargetModel model, TargetEmitter requestAdapter) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(requestAdapter, "requestAdapter");
        MigrationProfile.Language target = model.targetLanguage();
        TargetEmitter registered = emitters.get(target);
        boolean requestSupportsTarget = requestAdapter.supports(target);
        if (registered != null && requestSupportsTarget) {
            throw new DuplicateTargetEmitterException(target, List.of(registered, requestAdapter));
        }
        TargetEmitter selected = requestSupportsTarget ? requestAdapter : registered;
        if (selected == null) throw new TargetEmitterUnavailableException(target, availableTargets(requestAdapter));
        return selected.emit(model, model.profile());
    }

    public List<MigrationProfile.Language> availableTargets() {
        return emitters.keySet().stream().sorted(Comparator.comparing(Enum::name)).toList();
    }

    private List<MigrationProfile.Language> availableTargets(TargetEmitter requestAdapter) {
        List<MigrationProfile.Language> available = new ArrayList<>(emitters.keySet());
        for (MigrationProfile.Language target : MigrationProfile.Language.values()) {
            if (requestAdapter.supports(target) && !available.contains(target)) available.add(target);
        }
        return available.stream().sorted(Comparator.comparing(Enum::name)).toList();
    }

    public static final class TargetEmitterUnavailableException extends IllegalStateException {
        public static final String CODE = "TARGET_EMITTER_UNAVAILABLE";
        private final MigrationProfile.Language requestedTarget;
        private final List<MigrationProfile.Language> availableTargets;

        private TargetEmitterUnavailableException(MigrationProfile.Language requestedTarget,
                                                   Collection<MigrationProfile.Language> availableTargets) {
            super(CODE + ": requested=" + requestedTarget + ", available=" + ordered(availableTargets));
            this.requestedTarget = requestedTarget;
            this.availableTargets = ordered(availableTargets);
        }
        public String code() { return CODE; }
        public MigrationProfile.Language requestedTarget() { return requestedTarget; }
        public List<MigrationProfile.Language> availableTargets() { return availableTargets; }
    }

    public static final class DuplicateTargetEmitterException extends IllegalStateException {
        private final MigrationProfile.Language target;
        private final List<String> emitterTypes;
        private DuplicateTargetEmitterException(MigrationProfile.Language target, List<TargetEmitter> emitters) {
            super("Duplicate target emitters for " + target + ": " + emitters.stream()
                    .map(value -> value.getClass().getName()).sorted().toList());
            this.target = target;
            this.emitterTypes = emitters.stream().map(value -> value.getClass().getName()).sorted().toList();
        }
        public MigrationProfile.Language target() { return target; }
        public List<String> emitterTypes() { return emitterTypes; }
    }

    private static List<MigrationProfile.Language> ordered(Collection<MigrationProfile.Language> values) {
        return values.stream().sorted(Comparator.comparing(Enum::name)).toList();
    }
}
