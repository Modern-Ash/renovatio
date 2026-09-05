package org.shark.renovatio.domain.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** Executes a configured external runtime and captures an observable replay result. */
public final class ProcessReplayRunner implements ReplayRunner {
    private final List<String> command;
    private final Path workingDirectory;
    public ProcessReplayRunner(List<String> command, Path workingDirectory) {
        if (command == null || command.isEmpty()) throw new IllegalArgumentException("command is required");
        this.command = List.copyOf(command); this.workingDirectory = workingDirectory;
    }
    @Override public ReplayResult run(ReplayInput input) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            if (workingDirectory != null) builder.directory(workingDirectory.toFile());
            Process process = builder.start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            return new ReplayResult(exit == 0 ? "SUCCESS" : "FAILED",
                    java.util.Map.of("stdout", stdout, "exitCode", exit), java.util.List.of(), java.util.List.of(), stderr.isBlank() ? null : stderr);
        } catch (IOException error) { return new ReplayResult("FAILED", java.util.Map.of(), java.util.List.of(), java.util.List.of(), error.toString()); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); return new ReplayResult("INTERRUPTED", java.util.Map.of(), java.util.List.of(), java.util.List.of(), error.toString()); }
    }
}
