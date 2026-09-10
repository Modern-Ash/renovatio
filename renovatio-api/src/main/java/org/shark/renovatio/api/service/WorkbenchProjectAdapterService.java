package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.WorkbenchAssetDto;
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

    public List<WorkbenchAssetDto> list(Path root, boolean devWriteEnabled) throws IOException {
        try (var paths = Files.walk(root, 8)) {
            return paths.filter(Files::isRegularFile).sorted().map(path -> descriptor(root, path, devWriteEnabled)).toList();
        }
    }

    public String read(Path root, String assetId) throws IOException {
        return Files.readString(resolve(root, assetId));
    }

    public void write(Path root, String assetId, String content, boolean devWriteEnabled) throws IOException {
        Path asset = resolve(root, assetId);
        if (!devWriteEnabled || !isTarget(asset)) throw new SecurityException("Asset is read-only outside development target mode");
        Files.createDirectories(asset.getParent());
        Files.writeString(asset, content == null ? "" : content);
    }

    private WorkbenchAssetDto descriptor(Path root, Path asset, boolean devWriteEnabled) {
        String id = root.relativize(asset).toString().replace(asset.getFileSystem().getSeparator(), "/");
        return new WorkbenchAssetDto(id, asset.getFileName().toString(), category(asset), devWriteEnabled && isTarget(asset));
    }

    private Path resolve(Path root, String assetId) {
        if (assetId == null || assetId.isBlank()) throw new IllegalArgumentException("assetId is required");
        Path resolved = root.resolve(assetId).normalize();
        if (!resolved.startsWith(root)) throw new SecurityException("Asset path escapes workspace");
        return resolved;
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
