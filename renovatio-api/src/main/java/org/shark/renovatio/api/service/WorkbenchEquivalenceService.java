package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.shark.renovatio.api.dto.WorkbenchEquivalenceDto;
import org.springframework.stereotype.Service;

/** Projects existing workspace artifacts; it never performs or approves an equivalence comparison. */
@Service
public class WorkbenchEquivalenceService {
    private final ProjectService projects;
    private final WorkbenchProjectAdapterService assets;
    private final ObjectMapper json;

    public WorkbenchEquivalenceService(ProjectService projects, WorkbenchProjectAdapterService assets, ObjectMapper json) {
        this.projects = projects; this.assets = assets; this.json = json;
    }

    public WorkbenchEquivalenceDto summary(String projectId) throws Exception {
        Path root = projects.getProject(projectId)
                .map(project -> Path.of(project.getWorkspacePath()).toAbsolutePath().normalize())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        var all = assets.list(root, false);
        var evidence = items(all, "Evidence");
        return new WorkbenchEquivalenceDto(evidence, items(all, "Generated targets"), verdicts(root, evidence));
    }

    private List<WorkbenchEquivalenceDto.Item> items(List<org.shark.renovatio.api.dto.WorkbenchAssetDto> assets, String category) {
        return assets.stream()
                .filter(asset -> category.equals(asset.category()))
                .map(asset -> new WorkbenchEquivalenceDto.Item(asset.id(), asset.name()))
                .toList();
    }

    private List<WorkbenchEquivalenceDto.Verdict> verdicts(Path root, List<WorkbenchEquivalenceDto.Item> evidence) {
        return evidence.stream().filter(item -> item.id().endsWith(".json")).map(item -> verdict(root, item.id()))
                .flatMap(java.util.Optional::stream).toList();
    }

    private java.util.Optional<WorkbenchEquivalenceDto.Verdict> verdict(Path root, String id) {
        try {
            Path evidence = root.resolve(id).normalize();
            if (!evidence.startsWith(root) || !Files.isRegularFile(evidence)) return java.util.Optional.empty();
            var node = json.readTree(Files.readString(evidence));
            String fixture = node.path("fixtureId").asText();
            String classification = node.path("classification").asText();
            if (fixture.isBlank() || classification.isBlank()) return java.util.Optional.empty();
            return java.util.Optional.of(new WorkbenchEquivalenceDto.Verdict(fixture, classification,
                    node.path("reason").asText(), node.path("blocksRelease").asBoolean(false)));
        } catch (Exception ignored) { return java.util.Optional.empty(); }
    }
}
