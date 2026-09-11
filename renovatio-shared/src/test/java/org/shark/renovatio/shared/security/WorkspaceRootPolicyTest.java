package org.shark.renovatio.shared.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceRootPolicyTest {
    @TempDir Path temp;

    @Test
    void blocksTraversalAndAbsolutePathsOutsideAllowedRoots() {
        Path root = temp.resolve("allowed");
        WorkspaceRootPolicy policy = WorkspaceRootPolicy.under(root);
        Path outside = temp.resolve("outside").resolve("new-project");

        assertThrows(SecurityException.class, () -> policy.prepareWorkspace("../escape"));
        assertThrows(SecurityException.class, () -> policy.prepareWorkspace(outside.toString()));
        assertTrue(Files.notExists(outside.getParent()));
        assertThrows(SecurityException.class, () -> policy.resolveExisting(root, "/etc/passwd"));
    }

    @Test
    void resolvesOnlyExistingFilesInsideWorkspace() throws Exception {
        Path workspace = temp.resolve("allowed").resolve("project");
        Files.createDirectories(workspace);
        Files.writeString(workspace.resolve("A.CBL"), "IDENTIFICATION DIVISION.");
        WorkspaceRootPolicy policy = WorkspaceRootPolicy.under(temp.resolve("allowed"));

        assertTrue(policy.resolveExisting(workspace, "A.CBL").startsWith(workspace.toRealPath()));
        assertThrows(SecurityException.class, () -> policy.resolveExisting(workspace, "../A.CBL"));
    }

    @Test
    void rejectsSymlinkWorkspaceBoundary() throws Exception {
        Path root = temp.resolve("allowed");
        Files.createDirectories(root);
        Path outside = temp.resolve("outside");
        Files.createDirectories(outside);
        Path link = root.resolve("linked");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException exception) {
            return;
        }
        WorkspaceRootPolicy policy = WorkspaceRootPolicy.under(root);

        assertThrows(SecurityException.class, () -> policy.prepareWorkspace("linked"));
    }

    @Test
    void rejectsIntermediateSymlinkEscapes() throws Exception {
        Path root = temp.resolve("allowed");
        Path workspace = root.resolve("project");
        Files.createDirectories(workspace);
        Path outside = temp.resolve("outside");
        Files.createDirectories(outside.resolve("sub"));
        Files.writeString(outside.resolve("sub").resolve("secret.txt"), "secret");
        Path link = workspace.resolve("link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException exception) {
            return;
        }
        WorkspaceRootPolicy policy = WorkspaceRootPolicy.under(root);

        assertThrows(SecurityException.class, () -> policy.resolveExisting(workspace, "link/sub/secret.txt"));
        assertThrows(SecurityException.class, () -> policy.resolveForWrite(workspace, "link/sub/new.txt"));
    }
}
