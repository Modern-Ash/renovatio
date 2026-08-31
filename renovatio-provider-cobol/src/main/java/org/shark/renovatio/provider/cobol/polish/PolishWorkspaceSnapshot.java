package org.shark.renovatio.provider.cobol.polish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.HexFormat;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/** Reads only declared generated-Java paths and proves the proposal lane did not mutate them. */
final class PolishWorkspaceSnapshot {

    Map<String, String> generatedSourceHashes(Path workspace, PolishProposalRequest request) throws IOException {
        Path root = generatedRoot(workspace, request.generatedRoot());
        Map<String, String> hashes = new TreeMap<>();
        for (String relative : request.generatedSources().keySet()) {
            Path file = generatedFile(root, relative);
            if (!file.startsWith(root) || containsSymbolicLink(root, file)) {
                throw new IOException("Declared generated source escapes or aliases the generated root");
            }
            if (!Files.exists(file)) {
                throw new IOException("Declared generated source is missing: " + relative);
            }
            if (!Files.isRegularFile(file)) {
                throw new IOException("Declared generated source is not a file: " + file);
            }
            hashes.put(relative, PolishContracts.sha256(Files.readString(file)));
        }
        return hashes;
    }

    Map<String, byte[]> snapshotGeneratedSources(Path workspace, PolishProposalRequest request)
            throws IOException {
        Path root = generatedRoot(workspace, request.generatedRoot());
        Map<String, byte[]> sourceBytes = new TreeMap<>();
        for (String relative : request.generatedSources().keySet()) {
            Path file = generatedFile(root, relative);
            if (!file.startsWith(root) || containsSymbolicLink(root, file)) {
                throw new IOException("Declared generated source escapes or aliases the generated root");
            }
            if (Files.exists(file)) {
                sourceBytes.put(relative, Files.readAllBytes(file));
            } else {
                sourceBytes.put(relative, null);
            }
        }
        return sourceBytes;
    }

    void restoreGeneratedSources(Path workspace, PolishProposalRequest request,
                                Map<String, byte[]> sourceBytes) throws IOException {
        Path root = generatedRoot(workspace, request.generatedRoot());
        for (Map.Entry<String, byte[]> entry : sourceBytes.entrySet()) {
            Path file = generatedFile(root, entry.getKey());
            if (!file.startsWith(root) || containsSymbolicLink(root, file)) {
                throw new IOException("Declared generated source escapes or aliases the generated root");
            }
            if (entry.getValue() == null) {
                Files.deleteIfExists(file);
            } else {
                Files.createDirectories(file.getParent());
                Files.write(file, entry.getValue(), CREATE, TRUNCATE_EXISTING, WRITE);
            }
        }
    }

    String hash(Path workspace, PolishProposalRequest request) throws IOException {
        Path root = generatedRoot(workspace, request.generatedRoot());
        if (!root.startsWith(workspace.toAbsolutePath()) || Files.isSymbolicLink(root)) {
            throw new IOException("Generated root escapes or aliases the workspace");
        }
        MessageDigest digest = sha256();
        for (String relative : request.pathSelectors().keySet()) {
            Path file = generatedFile(root, relative);
            if (!file.startsWith(root) || containsSymbolicLink(root, file)) {
                throw new IOException("Generated Java path escapes or aliases its declared root");
            }
            digest.update(relative.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            if (Files.exists(file)) {
                if (!Files.isRegularFile(file)) throw new IOException("Generated Java path is not a file");
                digest.update((byte) 1);
                digest.update(Files.readAllBytes(file));
            } else {
                digest.update((byte) 2);
            }
            digest.update((byte) 0xff);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private Path generatedRoot(Path workspace, String generatedRoot) throws IOException {
        Path root = workspace.toAbsolutePath().resolve(generatedRoot).normalize();
        if (!root.startsWith(workspace.toAbsolutePath())) {
            throw new IOException("Generated root escapes the workspace");
        }
        if (Files.isSymbolicLink(root) || containsSymbolicLink(workspace.toAbsolutePath(), root)) {
            throw new IOException("Generated root escapes or aliases the workspace");
        }
        return root;
    }

    private Path generatedFile(Path root, String relative) {
        return root.resolve(relative).normalize();
    }

    private boolean containsSymbolicLink(Path root, Path file) {
        Path current = root;
        Path relative = root.relativize(file);
        for (Path part : relative) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) return true;
        }
        return false;
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
