package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto;
import org.shark.renovatio.api.dto.WorkbenchShadowImpactDto;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Builds a deterministic, read-only shadow diff and impact report before generation. */
@Service
public class WorkbenchShadowImpactService {
    private static final List<String> TARGET_EXTENSIONS = List.of(".java", ".py", ".js", ".ts", ".mjs", ".cjs");

    private final ProjectRepository projects;
    private final WorkbenchSourceExplorerService sourceExplorer;
    private final WorkbenchDomainModelService domainModels;
    private final WorkbenchArchitectureCanvasService architectureCanvas;

    public WorkbenchShadowImpactService(ProjectRepository projects,
                                        WorkbenchSourceExplorerService sourceExplorer,
                                        WorkbenchDomainModelService domainModels,
                                        WorkbenchArchitectureCanvasService architectureCanvas) {
        this.projects = projects;
        this.sourceExplorer = sourceExplorer;
        this.domainModels = domainModels;
        this.architectureCanvas = architectureCanvas;
    }

    public WorkbenchShadowImpactDto summary(String projectId) throws IOException {
        ProjectEntity project = projects.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        Path root = Path.of(project.getWorkspacePath()).toAbsolutePath().normalize();
        WorkbenchSourceExplorerDto source = sourceExplorer.explore(root);
        WorkbenchDomainModelDto domain = domainModels.read(projectId);
        WorkbenchArchitectureCanvasDto architecture = architectureCanvas.read(projectId);

        List<String> planned = architecture.manifest().stream().map(WorkbenchArchitectureCanvasDto.ManifestEntry::path)
                .distinct().sorted().toList();
        List<String> existingTargets = existingTargets(root);
        Set<String> plannedSet = new LinkedHashSet<>(planned);
        Set<String> existingSet = new LinkedHashSet<>(existingTargets);
        List<String> added = planned.stream().filter(path -> !existingSet.contains(path)).toList();
        List<String> removed = existingTargets.stream().filter(path -> !plannedSet.contains(path)).toList();
        List<String> changed = planned.stream().filter(existingSet::contains).toList();

        Map<String, List<String>> domainBySource = domainBySource(domain.model());
        Map<String, List<String>> evidenceByDomain = evidenceByDomain(domain.model());
        List<WorkbenchShadowImpactDto.ArtifactImpact> artifacts = artifactImpacts(architecture, domainBySource,
                evidenceByDomain, existingSet);
        Map<String, List<String>> artifactsBySource = artifactsBySource(artifacts);
        List<WorkbenchShadowImpactDto.SourceImpact> sources = source.files().stream()
                .map(file -> new WorkbenchShadowImpactDto.SourceImpact(file.path(), file.kind(), file.symbols().size(),
                        domainBySource.getOrDefault(file.path(), List.of()),
                        artifactsBySource.getOrDefault(file.path(), List.of())))
                .sorted(Comparator.comparing(WorkbenchShadowImpactDto.SourceImpact::sourcePath))
                .toList();
        List<String> unresolvedEvidence = unresolvedEvidence(domainBySource.keySet(), source.files());
        WorkbenchShadowImpactDto.DiffSummary diff = new WorkbenchShadowImpactDto.DiffSummary(planned,
                existingTargets, added, removed, changed, unresolvedEvidence);
        String canonical = MigrationProfiles.canonical(Map.of(
                "sourceHash", sourceHash(source.files()),
                "domainHash", domain.canonicalHash(),
                "architectureHash", architecture.canonicalHash(),
                "diff", diff,
                "artifacts", artifacts));
        String hash = "sha256:" + MigrationProfiles.sha256(canonical);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", "1");
        report.put("canonicalHash", hash);
        report.put("projectId", projectId);
        report.put("source", stage("COBOL", 0, sourceHash(source.files()), source.files().size(), source.files().isEmpty() ? "empty" : "ready"));
        report.put("domain", stage("DomainModel", domain.revision(), domain.canonicalHash(), domain.model().nodes().size(), "ready"));
        report.put("architecture", stage("Architecture", architecture.revision(), architecture.canonicalHash(), architecture.manifest().size(), "ready"));
        report.put("diff", diff);
        report.put("sourceImpacts", sources);
        report.put("artifactImpacts", artifacts);
        return new WorkbenchShadowImpactDto("1", hash,
                stage("COBOL", 0, sourceHash(source.files()), source.files().size(), source.files().isEmpty() ? "empty" : "ready"),
                stage("DomainModel", domain.revision(), domain.canonicalHash(), domain.model().nodes().size(), "ready"),
                stage("Architecture", architecture.revision(), architecture.canonicalHash(), architecture.manifest().size(), "ready"),
                sources, artifacts, diff, Map.copyOf(report));
    }

    private WorkbenchShadowImpactDto.Stage stage(String name, long revision, String hash, int itemCount, String status) {
        return new WorkbenchShadowImpactDto.Stage(name, revision, hash, itemCount, status);
    }

    private List<WorkbenchShadowImpactDto.ArtifactImpact> artifactImpacts(WorkbenchArchitectureCanvasDto architecture,
            Map<String, List<String>> domainBySource, Map<String, List<String>> evidenceByDomain,
            Set<String> existingTargets) {
        List<String> allDomainIds = evidenceByDomain.keySet().stream().sorted().toList();
        List<String> allEvidence = evidenceByDomain.values().stream().flatMap(List::stream).distinct().sorted().toList();
        List<String> allSources = domainBySource.keySet().stream().sorted().toList();
        return architecture.manifest().stream().map(entry -> {
            List<String> domainIds = domainIdsFor(entry, allDomainIds);
            if (domainIds.isEmpty() && !allDomainIds.isEmpty()) domainIds = allDomainIds;
            List<String> evidence = domainIds.stream().flatMap(id -> evidenceByDomain.getOrDefault(id, List.of()).stream())
                    .distinct().sorted().toList();
            if (evidence.isEmpty()) evidence = allEvidence;
            List<String> sourceRefs = evidence.stream().map(WorkbenchShadowImpactService::sourcePath)
                    .filter(value -> !value.isBlank()).distinct().sorted().toList();
            if (sourceRefs.isEmpty()) sourceRefs = allSources;
            String status = existingTargets.contains(entry.path()) ? "present-in-workspace" : "planned-new";
            String determinism = evidence.isEmpty() ? "inferred" : "deterministic";
            return new WorkbenchShadowImpactDto.ArtifactImpact(entry.path(), entry.role(), entry.layer(),
                    entry.componentId(), status, determinism, sourceRefs, domainIds, evidence);
        }).sorted(Comparator.comparing(WorkbenchShadowImpactDto.ArtifactImpact::path)).toList();
    }

    private static List<String> domainIdsFor(WorkbenchArchitectureCanvasDto.ManifestEntry entry, List<String> candidates) {
        String key = normalize(entry.className() + " " + entry.role() + " " + entry.layer());
        return candidates.stream().filter(id -> key.contains(normalize(id))).toList();
    }

    private static Map<String, List<String>> artifactsBySource(List<WorkbenchShadowImpactDto.ArtifactImpact> artifacts) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        artifacts.forEach(artifact -> artifact.sourceRefs().forEach(source ->
                result.computeIfAbsent(source, ignored -> new ArrayList<>()).add(artifact.path())));
        result.replaceAll((key, value) -> value.stream().distinct().sorted().toList());
        return Map.copyOf(result);
    }

    private static Map<String, List<String>> domainBySource(DomainModel model) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        model.nodes().forEach(node -> collectEvidence(result, node.id(), node.evidence()));
        model.nodes().forEach(node -> node.properties().forEach(property -> collectEvidence(result, node.id(), property.evidence())));
        model.invariants().forEach(invariant -> collectEvidence(result, invariant.id(), invariant.evidence()));
        result.replaceAll((key, value) -> value.stream().distinct().sorted().toList());
        return Map.copyOf(result);
    }

    private static Map<String, List<String>> evidenceByDomain(DomainModel model) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        model.nodes().forEach(node -> result.computeIfAbsent(node.id(), ignored -> new ArrayList<>())
                .addAll(node.evidence().stream().map(DomainModel.Evidence::sourceRef).toList()));
        model.nodes().forEach(node -> node.properties().forEach(property ->
                result.computeIfAbsent(node.id(), ignored -> new ArrayList<>())
                        .addAll(property.evidence().stream().map(DomainModel.Evidence::sourceRef).toList())));
        model.invariants().forEach(invariant -> result.computeIfAbsent(invariant.id(), ignored -> new ArrayList<>())
                .addAll(invariant.evidence().stream().map(DomainModel.Evidence::sourceRef).toList()));
        result.replaceAll((key, value) -> value.stream().distinct().sorted().toList());
        return Map.copyOf(result);
    }

    private static void collectEvidence(Map<String, List<String>> result, String domainId,
                                        List<DomainModel.Evidence> evidence) {
        evidence.forEach(item -> result.computeIfAbsent(sourcePath(item.sourceRef()), ignored -> new ArrayList<>())
                .add(domainId));
    }

    private static List<String> unresolvedEvidence(Set<String> evidenceSources,
                                                   List<WorkbenchSourceExplorerDto.SourceFile> files) {
        Set<String> known = new LinkedHashSet<>();
        files.forEach(file -> known.add(file.path()));
        return evidenceSources.stream().filter(source -> !known.contains(source)).sorted().toList();
    }

    private static List<String> existingTargets(Path root) throws IOException {
        if (!Files.exists(root)) return List.of();
        try (Stream<Path> paths = Files.walk(root, 8)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> TARGET_EXTENSIONS.contains(extension(path)))
                    .map(path -> root.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/"))
                    .sorted().toList();
        }
    }

    private static String sourceHash(List<WorkbenchSourceExplorerDto.SourceFile> files) {
        return "sha256:" + MigrationProfiles.sha256(MigrationProfiles.canonical(files.stream()
                .map(file -> Map.of("path", file.path(), "hash", file.hash(), "symbols", file.symbols().size()))
                .toList()));
    }

    private static String sourcePath(String sourceRef) {
        if (sourceRef == null) return "";
        int marker = sourceRef.indexOf('#');
        return marker < 0 ? sourceRef : sourceRef.substring(0, marker);
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
