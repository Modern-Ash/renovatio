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
        List<String> existingTargets = existingTargets(outputRoot(root, project));
        Set<String> plannedSet = new LinkedHashSet<>(planned);
        Set<String> existingSet = new LinkedHashSet<>(existingTargets);
        List<String> added = planned.stream().filter(path -> !existingSet.contains(path)).toList();
        List<String> removed = existingTargets.stream().filter(path -> !plannedSet.contains(path)).toList();
        List<String> changed = planned.stream().filter(existingSet::contains).toList();

        Map<String, List<String>> domainBySource = domainBySource(domain.model());
        Map<String, DomainEvidence> evidenceByDomain = evidenceByDomain(domain.model());
        List<WorkbenchShadowImpactDto.ArtifactImpact> artifacts = artifactImpacts(architecture, evidenceByDomain,
                existingSet);
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
            Map<String, DomainEvidence> evidenceByDomain, Set<String> existingTargets) {
        List<DomainEvidence> candidates = evidenceByDomain.values().stream()
                .sorted(Comparator.comparing(DomainEvidence::id)).toList();
        return architecture.manifest().stream().map(entry -> {
            List<DomainEvidence> matches = domainEvidenceFor(entry, candidates);
            List<String> domainIds = matches.stream().map(DomainEvidence::id).distinct().sorted().toList();
            List<String> evidence = matches.stream().flatMap(match -> match.evidenceRefs().stream())
                    .distinct().sorted().toList();
            List<String> sourceRefs = evidence.stream().map(WorkbenchShadowImpactService::sourcePath)
                    .filter(value -> !value.isBlank()).distinct().sorted().toList();
            String status = existingTargets.contains(entry.path()) ? "present-in-workspace" : "planned-new";
            String determinism = domainIds.isEmpty() || evidence.isEmpty() ? "inferred" : "deterministic";
            return new WorkbenchShadowImpactDto.ArtifactImpact(entry.path(), entry.role(), entry.layer(),
                    entry.componentId(), status, determinism, sourceRefs, domainIds, evidence);
        }).sorted(Comparator.comparing(WorkbenchShadowImpactDto.ArtifactImpact::path)).toList();
    }

    private static List<DomainEvidence> domainEvidenceFor(WorkbenchArchitectureCanvasDto.ManifestEntry entry,
                                                          List<DomainEvidence> candidates) {
        String key = normalize(entry.className() + " " + entry.role() + " " + entry.layer());
        return candidates.stream().filter(candidate -> key.contains(normalize(candidate.id()))
                || key.contains(normalize(candidate.name()))).toList();
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

    private static Map<String, DomainEvidence> evidenceByDomain(DomainModel model) {
        Map<String, DomainEvidence> result = new LinkedHashMap<>();
        model.nodes().forEach(node -> {
            List<String> evidence = new ArrayList<>(node.evidence().stream().map(DomainModel.Evidence::sourceRef).toList());
            node.properties().forEach(property ->
                    evidence.addAll(property.evidence().stream().map(DomainModel.Evidence::sourceRef).toList()));
            result.put(node.id(), new DomainEvidence(node.id(), node.name(), evidence.stream().distinct().sorted().toList()));
        });
        model.invariants().forEach(invariant -> result.put(invariant.id(),
                new DomainEvidence(invariant.id(), invariant.expression(),
                        invariant.evidence().stream().map(DomainModel.Evidence::sourceRef).distinct().sorted().toList())));
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

    private static Path outputRoot(Path workspaceRoot, ProjectEntity project) {
        String configured = project.getJavaOutputPath();
        if (configured == null || configured.isBlank()) return workspaceRoot.resolve("generated-java-stubs").normalize();
        Path path = Path.of(configured.trim());
        return path.isAbsolute() ? path.normalize() : workspaceRoot.resolve(path).normalize();
    }

    private static List<String> existingTargets(Path outputRoot) throws IOException {
        if (!Files.exists(outputRoot)) return List.of();
        try (Stream<Path> paths = Files.walk(outputRoot, 8)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> TARGET_EXTENSIONS.contains(extension(path)))
                    .map(path -> outputRoot.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/"))
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

    private record DomainEvidence(String id, String name, List<String> evidenceRefs) { }
}
