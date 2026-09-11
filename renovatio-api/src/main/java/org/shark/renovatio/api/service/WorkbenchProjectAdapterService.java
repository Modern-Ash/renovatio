package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.WorkbenchAssetDto;
import org.shark.renovatio.shared.security.WorkspaceRootPolicy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Service
public class WorkbenchProjectAdapterService {
    private static final List<String> LEGACY = List.of(".cbl", ".cob", ".cpy", ".jcl");
    private static final List<String> TARGET = List.of(".java", ".py", ".js", ".ts", ".mjs", ".cjs");
    private final WorkspaceRootPolicy workspaceRootPolicy;

    public WorkbenchProjectAdapterService(WorkspaceRootPolicy workspaceRootPolicy) {
        this.workspaceRootPolicy = workspaceRootPolicy;
    }

    public List<WorkbenchAssetDto> list(Path root, boolean devWriteEnabled) throws IOException {
        Path workspace = workspaceRootPolicy.resolveForWrite(root, ".");
        try (var paths = Files.walk(workspace, 8)) {
            return paths.filter(Files::isRegularFile).sorted().map(path -> descriptor(workspace, path, devWriteEnabled)).toList();
        }
    }

    public String read(Path root, String assetId) throws IOException {
        return Files.readString(workspaceRootPolicy.resolveExisting(root, assetId));
    }

    public void write(Path root, String assetId, String content, boolean devWriteEnabled) throws IOException {
        if (!devWriteEnabled || !isTarget(Path.of(assetId))) throw new SecurityException("Asset is read-only outside development target mode");
        Path asset = workspaceRootPolicy.resolveForWrite(root, assetId);
        Files.createDirectories(asset.getParent());
        Files.writeString(asset, content == null ? "" : content);
    }

    private WorkbenchAssetDto descriptor(Path root, Path asset, boolean devWriteEnabled) {
        String id = root.relativize(asset).toString().replace(asset.getFileSystem().getSeparator(), "/");
        return new WorkbenchAssetDto(id, asset.getFileName().toString(), category(asset), devWriteEnabled && isTarget(asset));
    }

    private boolean isTarget(Path path) { return TARGET.contains(extension(path)); }
    private String category(Path path) {
        String ext = extension(path);
        if (LEGACY.contains(ext)) return ext.equals(".cpy") ? "Copybooks" : ext.equals(".jcl") ? "JCL" : "COBOL sources";
        if (isTarget(path)) return "Generated targets";
        return "Evidence";
    }
    private String extension(Path path) { String n = path.getFileName().toString().toLowerCase(Locale.ROOT); int i = n.lastIndexOf('.'); return i < 0 ? "" : n.substring(i); }
}
