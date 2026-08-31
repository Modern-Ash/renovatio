package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;

final class PolishArtifactWriter {

    static final Path DEFAULT_ROOT = Path.of("build", "reports", "renovatio", "idiomatic-polish");

    private final ObjectMapper objectMapper;

    PolishArtifactWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    Path write(Path workspace, PolishProposalManifest manifest, String diff) throws IOException {
        Path root = workspace.toAbsolutePath().resolve(DEFAULT_ROOT).normalize();
        Path target = root.resolve(manifest.proposalId()).normalize();
        if (!target.startsWith(root) || !manifest.proposalId().matches("polish-[a-f0-9]{24}")) {
            throw new IllegalArgumentException("Invalid proposal artifact path");
        }
        byte[] patchBytes = diff.getBytes(StandardCharsets.UTF_8);
        byte[] manifestBytes = objectMapper.writeValueAsBytes(manifest);
        Files.createDirectories(root);

        if (Files.exists(target)) {
            verifyExisting(target, patchBytes, manifestBytes);
            return target;
        }

        Path temporary = Files.createTempDirectory(root, ".polish-");
        try {
            writeAndForce(temporary.resolve("proposal.patch"), patchBytes);
            writeAndForce(temporary.resolve("manifest.json"), manifestBytes);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            return target;
        } finally {
            deleteTree(temporary);
        }
    }

    private void verifyExisting(Path target, byte[] patchBytes, byte[] manifestBytes) throws IOException {
        try (var paths = Files.list(target)) {
            if (!paths.map(path -> path.getFileName().toString()).sorted().toList()
                    .equals(java.util.List.of("manifest.json", "proposal.patch"))) {
                throw new IOException("Content-addressed proposal directory has unexpected artifacts");
            }
        }
        if (!Arrays.equals(patchBytes, Files.readAllBytes(target.resolve("proposal.patch")))
                || !Arrays.equals(manifestBytes, Files.readAllBytes(target.resolve("manifest.json")))) {
            throw new IOException("Content-addressed proposal directory contains different bytes");
        }
    }

    private void writeAndForce(Path path, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }

    private void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
