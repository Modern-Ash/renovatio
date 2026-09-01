package org.shark.renovatio.shared.emission;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** One immutable path-keyed emitted artifact. */
public record EmittedArtifact(String path, byte[] content) {
    public EmittedArtifact {
        path = normalize(path);
        content = Objects.requireNonNull(content, "content").clone();
    }

    @Override
    public byte[] content() { return content.clone(); }

    @Override
    public boolean equals(Object candidate) {
        return candidate == this || candidate instanceof EmittedArtifact other
                && path.equals(other.path) && Arrays.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        return 31 * path.hashCode() + Arrays.hashCode(content);
    }

    public String utf8Text() { return new String(content, StandardCharsets.UTF_8); }

    public static EmittedArtifact utf8(String path, String content) {
        return new EmittedArtifact(path, Objects.requireNonNull(content, "content").getBytes(StandardCharsets.UTF_8));
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "path");
        String normalized = value.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*")) {
            throw new IllegalArgumentException("artifact path must be relative");
        }
        for (String segment : normalized.split("/", -1)) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("artifact path contains an invalid segment");
            }
        }
        return normalized;
    }
}
