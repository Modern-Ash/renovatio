package org.shark.renovatio.jcl.parse;

import java.util.Objects;

/** A workspace-relative JCL member supplied to the parser. */
public record JclSource(String path, String content) {
    public JclSource {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(content, "content");
        path = path.replace('\\', '/');
        if (path.isBlank() || path.startsWith("/") || path.contains("../"))
            throw new IllegalArgumentException("path must be workspace-relative");
    }
}
