package org.shark.renovatio.architecture;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

final class ArchitectureSupport {
    private ArchitectureSupport() { }

    static String text(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (normalized.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    static String hash(String value, String name) {
        String normalized = text(value, name).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be a lowercase SHA-256");
        }
        return normalized;
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static String id(String requestHash, String kind, String moduleId, String ownerId, String role) {
        return sha256(String.join("\n", "architecture.v1", hash(requestHash, "requestHash"),
                text(kind, "kind"), text(moduleId, "module"), text(ownerId, "owner"),
                text(role, "role")));
    }

    static String program(String value) {
        return text(value, "programId").toUpperCase(Locale.ROOT);
    }

    static String moduleName(String value) {
        String normalized = text(value, "moduleName").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (normalized.isBlank()) throw new IllegalArgumentException("moduleName must contain letters or digits");
        return normalized;
    }
}
