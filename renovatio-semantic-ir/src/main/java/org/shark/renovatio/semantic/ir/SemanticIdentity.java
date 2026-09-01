package org.shark.renovatio.semantic.ir;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/** Stable version-1 identity projection for semantic nodes. */
public final class SemanticIdentity {
    private SemanticIdentity() { }

    public static String nodeId(String programId, SemanticProgram.NodeKind kind,
                                SourceSpan span, String semanticRole) {
        String projection = String.join("\n", "semantic-ir.v1", normalizeProgramId(programId),
                Objects.requireNonNull(kind, "kind").name(), Objects.requireNonNull(span, "span").identity(),
                text(semanticRole, "semanticRole"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(projection.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public static String normalizeProgramId(String value) {
        return text(value, "programId").toUpperCase(Locale.ROOT);
    }

    static String text(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
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

    static String path(String value) {
        String normalized = text(value, "sourcePath").replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*")) {
            throw new IllegalArgumentException("sourcePath must be workspace-relative");
        }
        String[] segments = normalized.split("/");
        for (String segment : segments) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("sourcePath contains an invalid segment");
            }
        }
        return String.join("/", segments);
    }
}
