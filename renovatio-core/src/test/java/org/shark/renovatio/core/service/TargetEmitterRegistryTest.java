package org.shark.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;
import org.shark.renovatio.shared.emission.EmittedArtifact;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.spi.TargetEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TargetEmitterRegistryTest {
    @Test
    void resolvesAndInvokesExactlyOneJavaEmitter() {
        AtomicInteger calls = new AtomicInteger();
        TargetEmitter java = emitter(MigrationProfile.Language.JAVA, calls);
        TargetEmitterRegistry registry = new TargetEmitterRegistry(List.of(java));
        assertEquals("A", registry.emit(model(MigrationProfile.Language.JAVA)).artifacts().get(0).utf8Text());
        assertEquals(1, calls.get());
        assertEquals(List.of(MigrationProfile.Language.JAVA), registry.availableTargets());
    }

    @Test
    void unavailableTargetHasStableStructuredDetails() {
        TargetEmitterRegistry registry = new TargetEmitterRegistry(List.of(
                emitter(MigrationProfile.Language.JAVA, new AtomicInteger())));
        var error = assertThrows(TargetEmitterRegistry.TargetEmitterUnavailableException.class,
                () -> registry.resolve(MigrationProfile.Language.NODE));
        assertEquals("TARGET_EMITTER_UNAVAILABLE", error.code());
        assertEquals(MigrationProfile.Language.NODE, error.requestedTarget());
        assertEquals(List.of(MigrationProfile.Language.JAVA), error.availableTargets());
    }

    @Test
    void duplicateSupportFailsDeterministically() {
        var error = assertThrows(TargetEmitterRegistry.DuplicateTargetEmitterException.class,
                () -> new TargetEmitterRegistry(List.of(new BEmitter(), new AEmitter())));
        assertEquals(MigrationProfile.Language.JAVA, error.target());
        assertEquals(List.of(AEmitter.class.getName(), BEmitter.class.getName()), error.emitterTypes());
    }

    @Test
    void requestAdapterSupplementsRegisteredTargetsWithoutHidingThem() {
        AtomicInteger nodeCalls = new AtomicInteger();
        AtomicInteger javaCalls = new AtomicInteger();
        TargetEmitterRegistry registry = new TargetEmitterRegistry(List.of(
                emitter(MigrationProfile.Language.NODE, nodeCalls)));

        EmittedArtifacts result = registry.emit(model(MigrationProfile.Language.NODE),
                emitter(MigrationProfile.Language.JAVA, javaCalls));

        assertEquals("A", result.artifacts().get(0).utf8Text());
        assertEquals(1, nodeCalls.get());
        assertEquals(0, javaCalls.get());
    }

    @Test
    void requestAdapterParticipatesInDuplicateDetection() {
        TargetEmitterRegistry registry = new TargetEmitterRegistry(List.of(
                emitter(MigrationProfile.Language.JAVA, new AtomicInteger())));

        assertThrows(TargetEmitterRegistry.DuplicateTargetEmitterException.class,
                () -> registry.emit(model(MigrationProfile.Language.JAVA),
                        emitter(MigrationProfile.Language.JAVA, new AtomicInteger())));
    }

    private static TargetEmitter emitter(MigrationProfile.Language language, AtomicInteger calls) {
        return new TargetEmitter() {
            public boolean supports(MigrationProfile.Language target) { return target == language; }
            public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) {
                assertEquals(model.profile(), profile); calls.incrementAndGet();
                return EmittedArtifacts.of(List.of(EmittedArtifact.utf8("A.java", "A")));
            }
        };
    }

    private static TargetModel model(MigrationProfile.Language language) {
        MigrationProfile profile = MigrationProfiles.defaults();
        profile = new MigrationProfile(profile.schemaVersion(), profile.extensions(),
                new MigrationProfile.Target(language, "17"), profile.architecture(), profile.runtime(),
                profile.persistence(), profile.style(), profile.llm());
        var effective = MigrationProfiles.effective(profile, Map.of(), Map.of(), List.of());
        SourceSpan span = new SourceSpan("src/program.cob", 1, 1, 1, 9);
        SourceProvenance provenance = new SourceProvenance("src/program.cob", "0".repeat(64),
                "COBOL", Optional.empty(), List.of());
        SemanticProgram program = new SemanticProgram("1", SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.PROGRAM, "program", span), "TEST", provenance,
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
        return TargetModel.from(program, effective);
    }

    private static final class AEmitter implements TargetEmitter {
        public boolean supports(MigrationProfile.Language target) { return target == MigrationProfile.Language.JAVA; }
        public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) { return new EmittedArtifacts(List.of()); }
    }
    private static final class BEmitter implements TargetEmitter {
        public boolean supports(MigrationProfile.Language target) { return target == MigrationProfile.Language.JAVA; }
        public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) { return new EmittedArtifacts(List.of()); }
    }
}
