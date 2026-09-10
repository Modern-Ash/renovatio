package org.shark.renovatio.provider.cobol.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Content-addressed view of a generated tree, excluding build products. */
final class GeneratedTreeSnapshot {
    private GeneratedTreeSnapshot() {
    }

    static Map<String, String> capture(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return Map.of();
        }
        TreeMap<String, String> hashes = new TreeMap<>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                if (relative.equals("pom.xml") || relative.startsWith("target/")
                        || relative.startsWith(".git/")) {
                    continue;
                }
                hashes.put(relative, sha256(Files.readAllBytes(file)));
            }
        }
        return Map.copyOf(new LinkedHashMap<>(hashes));
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
