package org.shark.renovatio.provider.cobol.polish;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Reads only declared generated-Java paths and proves the proposal lane did not mutate them. */
final class PolishWorkspaceSnapshot {

    String hash(Path workspace, PolishProposalRequest request) throws IOException {
        Path root = workspace.resolve(request.generatedRoot()).normalize();
        if (!root.startsWith(workspace) || Files.isSymbolicLink(root)) {
            throw new IOException("Generated root escapes or aliases the workspace");
        }
        MessageDigest digest = sha256();
        for (String relative : request.pathSelectors().keySet()) {
            Path file = root.resolve(relative).normalize();
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
