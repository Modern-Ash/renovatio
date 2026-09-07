package org.shark.renovatio.shared.equivalence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/** External-runtime boundary for COBOL or generated targets; nonzero exits become explicit invariants. */
public final class CommandObservationRunner implements EquivalenceRunner {
    private final List<String> command;
    public CommandObservationRunner(List<String> command) { this.command = List.copyOf(command); }
    @Override public Observation run(Fixture fixture) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output = process.getInputStream().readAllBytes(); int exit = process.waitFor();
        String hash = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(output));
        return new Observation(hash, Map.of("exitCode", Integer.toString(exit), "fixture", fixture.id(), "output", new String(output, StandardCharsets.UTF_8)));
    }
}
