package org.shark.renovatio.shared.equivalence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;

/** Deterministic fixture adapter; executable runners may wrap it with observed invariants. */
public final class FileObservationRunner implements EquivalenceRunner {
    private final Path artifact; private final Map<String, String> invariants;
    public FileObservationRunner(Path artifact, Map<String, String> invariants) { this.artifact = artifact; this.invariants = invariants; }
    @Override public Observation run(Fixture fixture) throws Exception {
        byte[] bytes = Files.readAllBytes(artifact); byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        return new Observation(java.util.HexFormat.of().formatHex(digest), invariants);
    }
}
