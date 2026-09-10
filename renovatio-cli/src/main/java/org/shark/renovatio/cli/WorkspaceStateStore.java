package org.shark.renovatio.cli;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Filesystem-backed store for CLI-minted plan and run identifiers, kept under
 * {@code <workspace>/.renovatio/state/}. This lets a {@code plan} in one process be resolved by an
 * {@code apply} in a later process without adding persistence to the core services: each descriptor
 * captures the inputs needed to deterministically replay planning.
 */
public final class WorkspaceStateStore {

    /** Descriptor for a CLI-minted plan id. */
    public record PlanDescriptor(String planId, String workspacePath, String nql, String scope,
                                 String strategy, String framework, String createdAt) {
    }

    /** Descriptor for a CLI-minted run id. */
    public record RunDescriptor(String runId, String planId, String workspacePath, boolean dryRun,
                                String outputDir, String createdAt) {
    }

    private final ObjectMapper mapper = OutputWriter.mapper();
    private final Path stateDir;

    public WorkspaceStateStore(Path workspace) {
        this.stateDir = workspace.toAbsolutePath().resolve(".renovatio").resolve("state");
    }

    public void savePlan(PlanDescriptor descriptor) {
        write(stateDir.resolve("plans").resolve(descriptor.planId() + ".json"), descriptor);
    }

    public void saveRun(RunDescriptor descriptor) {
        write(stateDir.resolve("runs").resolve(descriptor.runId() + ".json"), descriptor);
    }

    public Optional<PlanDescriptor> loadPlan(String planId) {
        return read(stateDir.resolve("plans").resolve(planId + ".json"), PlanDescriptor.class);
    }

    public Optional<RunDescriptor> loadRun(String runId) {
        return read(stateDir.resolve("runs").resolve(runId + ".json"), RunDescriptor.class);
    }

    public static String now() {
        return Instant.now().toString();
    }

    private void write(Path file, Object value) {
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), value);
        } catch (IOException e) {
            throw new UncheckedIOException("could not write CLI state to " + file, e);
        }
    }

    private <T> Optional<T> read(Path file, Class<T> type) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(file.toFile(), type));
        } catch (IOException e) {
            throw new UncheckedIOException("could not read CLI state from " + file, e);
        }
    }
}
