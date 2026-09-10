package org.shark.renovatio.provider.cobol.service;

import org.shark.renovatio.shared.emission.EmittedArtifacts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Stages and validates a complete generated tree before replacing the visible output directory. */
final class GeneratedArtifactTreeWriter {

    Path write(Map<String, String> generatedFiles, Path requestedOutput) throws IOException {
        EmittedArtifacts artifacts = EmittedArtifacts.fromUtf8(Objects.requireNonNull(generatedFiles,
                "generatedFiles"));
        Path output = Objects.requireNonNull(requestedOutput, "requestedOutput").toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent == null || output.getFileName() == null) {
            throw new IllegalArgumentException("generated output must have a parent directory");
        }

        // Validate the whole resolved path set before creating or replacing anything.
        artifacts.artifacts().forEach(artifact -> {
            Path destination = output.resolve(artifact.path()).normalize();
            if (!destination.startsWith(output)) {
                throw new IllegalArgumentException("generated path escapes output directory: " + artifact.path());
            }
        });

        Files.createDirectories(parent);
        Path staging = Files.createTempDirectory(parent, "." + output.getFileName() + ".staging-");
        Path backup = parent.resolve("." + output.getFileName() + ".backup-" + UUID.randomUUID());
        boolean oldTreeMoved = false;
        try {
            for (var artifact : artifacts.artifacts()) {
                Path destination = staging.resolve(artifact.path()).normalize();
                Files.createDirectories(destination.getParent());
                Files.writeString(destination, artifact.utf8Text(), StandardCharsets.UTF_8);
            }
            if (Files.exists(output)) {
                atomicMove(output, backup);
                oldTreeMoved = true;
            }
            try {
                atomicMove(staging, output);
            } catch (IOException failure) {
                if (oldTreeMoved) atomicMove(backup, output);
                throw failure;
            }
            if (oldTreeMoved) deleteTree(backup);
            return output;
        } finally {
            if (Files.exists(staging)) deleteTree(staging);
            if (Files.exists(backup) && Files.exists(output)) deleteTree(backup);
        }
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            throw new IOException("atomic generated-tree replacement is not supported", unsupported);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
