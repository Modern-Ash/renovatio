package org.shark.renovatio.shared.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Canonical workspace boundary used by public adapters before touching files.
 */
public final class WorkspaceRootPolicy {
    private final List<Path> allowedRoots;

    public WorkspaceRootPolicy(List<Path> allowedRoots) {
        if (allowedRoots == null || allowedRoots.isEmpty()) {
            throw new IllegalArgumentException("at least one allowed workspace root is required");
        }
        this.allowedRoots = allowedRoots.stream()
                .map(WorkspaceRootPolicy::canonicalDirectory)
                .toList();
    }

    public static WorkspaceRootPolicy under(Path root) {
        return new WorkspaceRootPolicy(List.of(root));
    }

    public Path prepareWorkspace(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new IllegalArgumentException("workspacePath is required");
        }
        Path requested = Path.of(requestedPath.trim());
        Path absolute = requested.isAbsolute()
                ? requested.normalize()
                : allowedRoots.get(0).resolve(requested).normalize();
        ensureUnderAllowedRoot(absolute);
        rejectExistingSymlinkComponents(absolute);
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new SecurityException("workspace path has no parent");
        }
        Path canonicalParent = canonicalDirectory(parent);
        Path candidate = canonicalParent.resolve(absolute.getFileName()).normalize();
        ensureUnderAllowedRoot(candidate);
        rejectSymlink(candidate);
        try {
            Files.createDirectories(candidate);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to create workspace directory: " + candidate, exception);
        }
        return canonicalDirectory(candidate);
    }

    public Path resolveExisting(Path root, String relativePath) {
        Path workspace = canonicalDirectory(root);
        ensureUnderAllowedRoot(workspace);
        Path candidate = resolveInside(workspace, relativePath);
        try {
            Path real = candidate.toRealPath();
            ensureUnderAllowedRoot(real);
            return real;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Workspace path does not exist: " + candidate, exception);
        }
    }

    public Path resolveForWrite(Path root, String relativePath) {
        Path workspace = canonicalDirectory(root);
        ensureUnderAllowedRoot(workspace);
        Path candidate = resolveInside(workspace, relativePath);
        if (candidate.equals(workspace)) {
            return workspace;
        }
        Path parent = candidate.getParent();
        if (parent == null) {
            throw new SecurityException("path has no parent");
        }
        rejectExistingSymlinkComponents(parent);
        rejectSymlink(parent);
        Path canonicalParent = canonicalDirectory(parent);
        ensureUnderAllowedRoot(canonicalParent);
        rejectSymlink(candidate);
        return canonicalParent.resolve(candidate.getFileName()).normalize();
    }

    public List<Path> allowedRoots() {
        return new ArrayList<>(allowedRoots);
    }

    private Path resolveInside(Path root, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("path is required");
        }
        Path relative = Path.of(value);
        if (relative.isAbsolute()) {
            throw new SecurityException("absolute paths are not accepted inside workspaces");
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("path escapes workspace");
        }
        return resolved;
    }

    private void ensureUnderAllowedRoot(Path candidate) {
        boolean allowed = allowedRoots.stream().anyMatch(candidate::startsWith);
        if (!allowed) {
            throw new SecurityException("workspace path is outside allowed roots");
        }
    }

    private static void rejectSymlink(Path path) {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(path)) {
            throw new SecurityException("symbolic links are not accepted at workspace boundary");
        }
    }

    private static void rejectExistingSymlinkComponents(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path component : absolute) {
            current = current == null ? component : current.resolve(component);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new SecurityException("symbolic links are not accepted inside workspace paths");
            }
        }
    }

    private static Path canonicalDirectory(Path path) {
        try {
            Files.createDirectories(path);
            Path real = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!Files.isDirectory(real, LinkOption.NOFOLLOW_LINKS)) {
                throw new SecurityException("workspace root is not a directory");
            }
            return real.normalize();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to resolve workspace root: " + path, exception);
        }
    }
}
