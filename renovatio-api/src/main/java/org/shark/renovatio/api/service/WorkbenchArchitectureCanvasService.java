package org.shark.renovatio.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.shark.renovatio.api.dto.ArchitecturePreviewDto;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.ArchitectureProfileDraft;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.CanvasNode;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.Change;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.Comparison;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.DependencyDiagnostic;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.DependencyRule;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.ManifestEntry;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.Version;
import org.shark.renovatio.api.entity.ProjectArchitectureProfileVersionEntity;
import org.shark.renovatio.api.repository.ProjectArchitectureProfileVersionRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.architecture.ArchitectureGraph;
import org.shark.renovatio.architecture.ArchitectureLayoutOverrides;
import org.shark.renovatio.decisions.ProfileStore;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkbenchArchitectureCanvasService {
    static final String EXT_PACKAGE_PREFIX = ArchitectureLayoutOverrides.EXT_PACKAGE_PREFIX;
    static final String EXT_SUFFIX_PREFIX = ArchitectureLayoutOverrides.EXT_SUFFIX_PREFIX;
    static final String EXT_CLASS_PREFIX = ArchitectureLayoutOverrides.EXT_CLASS_PREFIX;
    static final String EXT_RULES = ArchitectureLayoutOverrides.EXT_RULES;
    private static final List<String> MVC_LAYERS = List.of("controller", "service", "model");

    private final ProjectRepository projects;
    private final ProjectArchitectureProfileVersionRepository versions;
    private final ProfileStore profiles;
    private final ArchitecturePreviewService previews;
    private final ObjectMapper json;

    public WorkbenchArchitectureCanvasService(ProjectRepository projects,
                                              ProjectArchitectureProfileVersionRepository versions,
                                              ProfileStore profiles,
                                              ArchitecturePreviewService previews,
                                              ObjectMapper json) {
        this.projects = projects;
        this.versions = versions;
        this.profiles = profiles;
        this.previews = previews;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public WorkbenchArchitectureCanvasDto read(String projectId) {
        requireProject(projectId);
        ProjectArchitectureProfileVersionEntity latest = latest(projectId);
        ArchitectureProfileDraft draft = latest == null ? fromProfile(profiles.find(projectId)
                .map(ProfileStore.VersionedProfile::profile).orElse(MigrationProfiles.emptyOverlay())) : deserialize(latest);
        return view(projectId, latest, draft);
    }

    @Transactional
    public WorkbenchArchitectureCanvasDto save(String projectId, long expectedRevision,
                                              ArchitectureProfileDraft candidate) {
        requireProject(projectId);
        ArchitectureProfileDraft normalized = normalize(candidate);
        ProjectArchitectureProfileVersionEntity current = latest(projectId);
        long currentRevision = current == null ? 0 : current.getRevision();
        if (expectedRevision != currentRevision) throw new RevisionConflictException(currentRevision);
        String canonical = canonicalHash(normalized);
        if (current != null && current.getCanonicalHash().equals(canonical)) return view(projectId, current, normalized);
        long nextRevision = currentRevision + 1;
        try {
            ProjectArchitectureProfileVersionEntity saved = versions.saveAndFlush(
                    new ProjectArchitectureProfileVersionEntity(projectId, nextRevision, canonical,
                            serialize(normalized), LocalDateTime.now()));
            replaceOverlay(projectId, normalized);
            return view(projectId, saved, normalized);
        } catch (DataIntegrityViolationException error) {
            throw new RevisionConflictException(latest(projectId) == null ? currentRevision : latest(projectId).getRevision());
        }
    }

    @Transactional(readOnly = true)
    public WorkbenchArchitectureCanvasDto preview(String projectId, ArchitectureProfileDraft candidate) {
        requireProject(projectId);
        ProjectArchitectureProfileVersionEntity current = latest(projectId);
        return view(projectId, current, normalize(candidate));
    }

    @Transactional(readOnly = true)
    public List<Version> versions(String projectId) {
        requireProject(projectId);
        return versions.findByProjectIdOrderByRevisionDesc(projectId).stream()
                .map(value -> new Version(value.getRevision(), hash(value.getCanonicalHash()),
                        value.getSavedAt(), deserialize(value).style())).toList();
    }

    @Transactional
    public WorkbenchArchitectureCanvasDto restore(String projectId, long revision, long expectedRevision) {
        requireProject(projectId);
        ArchitectureProfileDraft selected = deserialize(versions.findByProjectIdAndRevision(projectId, revision)
                .orElseThrow(() -> new NotFoundException("Architecture profile revision " + revision + " was not found")));
        return save(projectId, expectedRevision, selected);
    }

    @Transactional(readOnly = true)
    public Comparison compare(String projectId, long from, long to) {
        requireProject(projectId);
        ArchitectureProfileDraft before = deserialize(versions.findByProjectIdAndRevision(projectId, from)
                .orElseThrow(() -> new NotFoundException("Architecture profile revision " + from + " was not found")));
        ArchitectureProfileDraft after = deserialize(versions.findByProjectIdAndRevision(projectId, to)
                .orElseThrow(() -> new NotFoundException("Architecture profile revision " + to + " was not found")));
        Map<String, Object> left = flatten(before);
        Map<String, Object> right = flatten(after);
        List<Change> added = new ArrayList<>();
        List<Change> removed = new ArrayList<>();
        List<Change> changed = new ArrayList<>();
        right.forEach((key, value) -> {
            if (!left.containsKey(key)) added.add(new Change("profile", key, null, value));
            else if (!Objects.equals(value, left.get(key))) changed.add(new Change("profile", key, left.get(key), value));
        });
        left.forEach((key, value) -> { if (!right.containsKey(key)) removed.add(new Change("profile", key, value, null)); });
        Comparator<Change> order = Comparator.comparing(Change::targetId);
        added.sort(order); removed.sort(order); changed.sort(order);
        return new Comparison(List.copyOf(added), List.copyOf(removed), List.copyOf(changed));
    }

    private WorkbenchArchitectureCanvasDto view(String projectId, ProjectArchitectureProfileVersionEntity entity,
                                                ArchitectureProfileDraft draft) {
        ArchitecturePreviewDto preview = previews.preview(projectId, draft.style(), draft.moduleGrouping());
        List<CanvasNode> canvas = canvas(preview, draft);
        List<DependencyDiagnostic> diagnostics = dependencyDiagnostics(preview, draft.dependencyRules());
        return new WorkbenchArchitectureCanvasDto(entity == null ? 0 : entity.getRevision(),
                hash(canonicalHash(draft)), entity == null ? null : entity.getSavedAt(), draft, preview,
                canvas, draft.dependencyRules(), diagnostics, manifest(preview, draft, canvas));
    }

    private List<CanvasNode> canvas(ArchitecturePreviewDto preview, ArchitectureProfileDraft draft) {
        Map<String, String> packages = draft.packageRoots();
        Map<String, String> suffixes = draft.suffixes();
        return preview.components().stream().map(component -> {
            String layer = layer(component.kind().name(), component.name());
            String className = draft.classNames().getOrDefault(component.name(),
                    draft.classNames().getOrDefault(layer, classBase(component.name()) + suffixes.getOrDefault(layer, "")));
            return new CanvasNode(component.id(), layer, component.kind().name(), component.name(),
                    packages.getOrDefault(layer, packages.get("base")), className, component.id());
        }).sorted(Comparator.comparing(CanvasNode::layer).thenComparing(CanvasNode::label)).toList();
    }

    private List<ManifestEntry> manifest(ArchitecturePreviewDto preview, ArchitectureProfileDraft draft,
                                         List<CanvasNode> canvas) {
        Map<String, CanvasNode> byComponent = canvas.stream()
                .collect(Collectors.toMap(CanvasNode::componentId, Function.identity(), (left, right) -> left));
        return preview.artifacts().stream().map(artifact -> {
            CanvasNode node = byComponent.get(artifact.componentId());
            String layer = node == null ? layer(artifact.role(), artifact.path()) : node.layer();
            String packageName = draft.packageRoots().getOrDefault(layer, draft.packageRoots().get("base"));
            String className = node == null ? classBase(artifact.path().replace(".java", "")) : node.className();
            return new ManifestEntry(path(packageName, className), artifact.role(), layer, className, packageName,
                    artifact.componentId());
        }).sorted(Comparator.comparing(ManifestEntry::path)).toList();
    }

    private List<DependencyDiagnostic> dependencyDiagnostics(ArchitecturePreviewDto preview, List<DependencyRule> rules) {
        return preview.relations().stream().flatMap(relation -> {
            var from = preview.components().stream().filter(component -> component.id().equals(relation.fromComponentId())).findFirst();
            var to = preview.components().stream().filter(component -> component.id().equals(relation.toComponentId())).findFirst();
            if (from.isEmpty() || to.isEmpty()) return java.util.stream.Stream.<DependencyDiagnostic>empty();
            String fromLayer = layer(from.orElseThrow().kind().name(), from.orElseThrow().name());
            String toLayer = layer(to.orElseThrow().kind().name(), to.orElseThrow().name());
            boolean allowed = rules.stream().filter(rule -> rule.fromLayer().equals(fromLayer)
                    && rule.toLayer().equals(toLayer)).findFirst().map(DependencyRule::allowed).orElse(true);
            if (allowed) return java.util.stream.Stream.<DependencyDiagnostic>empty();
            return java.util.stream.Stream.of(new DependencyDiagnostic("error", "FORBIDDEN_DEPENDENCY",
                    fromLayer, toLayer, fromLayer + " must not depend on " + toLayer));
        }).distinct().sorted(Comparator.comparing(DependencyDiagnostic::fromLayer)
                .thenComparing(DependencyDiagnostic::toLayer)).toList();
    }

    private void replaceOverlay(String projectId, ArchitectureProfileDraft draft) {
        ProfileStore.VersionedProfile current = profiles.find(projectId)
                .orElse(new ProfileStore.VersionedProfile(MigrationProfiles.emptyOverlay(), 0));
        MigrationProfile overlay = toProfile(current.profile(), draft);
        profiles.replace(projectId, overlay, current.revision());
    }

    private ArchitectureProfileDraft fromProfile(MigrationProfile profile) {
        MigrationProfile resolved = MigrationProfiles.resolve(profile);
        Map<String, Object> extensions = profile.extensions() == null ? Map.of() : profile.extensions();
        return normalize(new ArchitectureProfileDraft(resolved.architecture().style(),
                resolved.architecture().moduleGrouping(), resolved.runtime().framework(),
                resolved.persistence().defaultStrategy(), map(extensions, EXT_PACKAGE_PREFIX, defaultsPackages()),
                map(extensions, EXT_SUFFIX_PREFIX, defaultsSuffixes(resolved.architecture().style())),
                map(extensions, EXT_CLASS_PREFIX, Map.of()), rules(extensions.get(EXT_RULES))));
    }

    private MigrationProfile toProfile(MigrationProfile current, ArchitectureProfileDraft draft) {
        Map<String, Object> extensions = new LinkedHashMap<>(current.extensions() == null ? Map.of() : current.extensions());
        extensions.keySet().removeIf(key -> key.startsWith(EXT_PACKAGE_PREFIX) || key.startsWith(EXT_SUFFIX_PREFIX)
                || key.startsWith(EXT_CLASS_PREFIX) || key.equals(EXT_RULES));
        draft.packageRoots().forEach((key, value) -> extensions.put(EXT_PACKAGE_PREFIX + key, value));
        draft.suffixes().forEach((key, value) -> extensions.put(EXT_SUFFIX_PREFIX + key, value));
        draft.classNames().forEach((key, value) -> extensions.put(EXT_CLASS_PREFIX + key, value));
        extensions.put(EXT_RULES, draft.dependencyRules().stream().map(rule -> Map.of("fromLayer", rule.fromLayer(),
                "toLayer", rule.toLayer(), "allowed", rule.allowed(), "reason", rule.reason())).toList());
        return new MigrationProfile(MigrationProfiles.SCHEMA_VERSION, extensions, current.target(),
                new MigrationProfile.Architecture(draft.style(), draft.moduleGrouping()),
                new MigrationProfile.Runtime(draft.framework()),
                new MigrationProfile.Persistence(draft.persistence(),
                        current.persistence() == null ? null : current.persistence().transactionBoundary(),
                        current.persistence() == null ? null : current.persistence().sourceStrategies()),
                current.style(), current.llm());
    }

    private ArchitectureProfileDraft normalize(ArchitectureProfileDraft draft) {
        if (draft == null) throw validation("PROFILE_REQUIRED", "profile", "Architecture profile is required");
        var style = draft.style() == null ? MigrationProfile.ArchitectureStyle.LAYERED_MVC : draft.style();
        var grouping = draft.moduleGrouping() == null ? MigrationProfile.ModuleGrouping.BY_PROGRAM : draft.moduleGrouping();
        var framework = draft.framework() == null ? MigrationProfile.Framework.SPRING_BOOT : draft.framework();
        var persistence = draft.persistence() == null ? MigrationProfile.PersistenceStrategy.IN_MEMORY : draft.persistence();
        Map<String, String> packages = merge(defaultsPackages(), draft.packageRoots());
        Map<String, String> suffixes = merge(defaultsSuffixes(style), draft.suffixes());
        Map<String, String> classes = clean(draft.classNames());
        List<DependencyRule> rules = draft.dependencyRules() == null || draft.dependencyRules().isEmpty()
                ? defaultRules(style) : draft.dependencyRules().stream().map(this::normalizeRule).toList();
        return new ArchitectureProfileDraft(style, grouping, framework, persistence, packages, suffixes, classes, rules);
    }

    private DependencyRule normalizeRule(DependencyRule rule) {
        if (rule == null || blank(rule.fromLayer()) || blank(rule.toLayer())) {
            throw validation("INVALID_DEPENDENCY_RULE", "dependencyRules", "Dependency rules require source and target layers");
        }
        return new DependencyRule(key(rule.fromLayer()), key(rule.toLayer()), rule.allowed(),
                blank(rule.reason()) ? "Architecture policy" : rule.reason().trim());
    }

    @SuppressWarnings("unchecked")
    private List<DependencyRule> rules(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(Map.class::cast).map(raw -> new DependencyRule(
                String.valueOf(raw.get("fromLayer")), String.valueOf(raw.get("toLayer")),
                Boolean.parseBoolean(String.valueOf(raw.get("allowed"))), String.valueOf(raw.get("reason")))).toList();
    }

    private Map<String, String> map(Map<String, Object> extensions, String prefix, Map<String, String> defaults) {
        Map<String, String> result = new LinkedHashMap<>(defaults);
        extensions.forEach((key, value) -> {
            if (key.startsWith(prefix) && value != null && !String.valueOf(value).isBlank()) {
                result.put(key(key.substring(prefix.length())), String.valueOf(value));
            }
        });
        return result;
    }

    private Map<String, String> merge(Map<String, String> defaults, Map<String, String> values) {
        Map<String, String> result = new LinkedHashMap<>(defaults);
        clean(values).forEach(result::put);
        return Map.copyOf(result);
    }

    private Map<String, String> clean(Map<String, String> values) {
        if (values == null) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((key, value) -> { if (!blank(key) && !blank(value)) result.put(key(key), value.trim()); });
        return Map.copyOf(result);
    }

    private Map<String, String> defaultsPackages() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("base", "org.shark.renovatio.generated");
        values.put("controller", "org.shark.renovatio.generated.controller");
        values.put("service", "org.shark.renovatio.generated.service");
        values.put("model", "org.shark.renovatio.generated.model");
        values.put("adapter", "org.shark.renovatio.generated.adapter");
        values.put("repository", "org.shark.renovatio.generated.repository");
        values.put("port", "org.shark.renovatio.generated.port");
        return Map.copyOf(values);
    }

    private Map<String, String> defaultsSuffixes(MigrationProfile.ArchitectureStyle style) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("controller", "Controller");
        values.put("service", "Service");
        values.put("model", style == MigrationProfile.ArchitectureStyle.LAYERED_MVC ? "Model" : "");
        values.put("adapter", "Adapter");
        values.put("repository", "Repository");
        values.put("port", "Port");
        return Map.copyOf(values);
    }

    private List<DependencyRule> defaultRules(MigrationProfile.ArchitectureStyle style) {
        if (style == MigrationProfile.ArchitectureStyle.LAYERED_MVC || style == MigrationProfile.ArchitectureStyle.LAYERED) {
            return List.of(new DependencyRule("controller", "service", true, "controllers invoke services"),
                    new DependencyRule("service", "model", true, "services use models"),
                    new DependencyRule("model", "controller", false, "models must stay framework independent"),
                    new DependencyRule("model", "service", false, "models must not call services"));
        }
        if (style == MigrationProfile.ArchitectureStyle.CLEAN || style == MigrationProfile.ArchitectureStyle.HEXAGONAL) {
            return List.of(new DependencyRule("adapter", "port", true, "adapters implement ports"),
                    new DependencyRule("service", "port", true, "application services use ports"),
                    new DependencyRule("model", "adapter", false, "domain must not depend on adapters"),
                    new DependencyRule("service", "controller", false, "application must not depend on inbound adapters"));
        }
        return List.of(new DependencyRule("service", "model", true, "transaction script services use data models"),
                new DependencyRule("model", "service", false, "data models do not invoke scripts"));
    }

    private Map<String, Object> flatten(ArchitectureProfileDraft draft) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("style", draft.style().name());
        result.put("moduleGrouping", draft.moduleGrouping().name());
        result.put("framework", draft.framework().name());
        result.put("persistence", draft.persistence().name());
        draft.packageRoots().forEach((key, value) -> result.put("packageRoots." + key, value));
        draft.suffixes().forEach((key, value) -> result.put("suffixes." + key, value));
        draft.classNames().forEach((key, value) -> result.put("classNames." + key, value));
        for (DependencyRule rule : draft.dependencyRules()) {
            result.put("dependencyRules." + rule.fromLayer() + "." + rule.toLayer(), rule.allowed() + ":" + rule.reason());
        }
        return result;
    }

    private String canonicalHash(ArchitectureProfileDraft draft) {
        return MigrationProfiles.sha256(MigrationProfiles.canonical(flatten(draft)));
    }

    private String serialize(ArchitectureProfileDraft profile) {
        try { return json.writeValueAsString(profile); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Unable to serialize architecture profile", error); }
    }

    private ArchitectureProfileDraft deserialize(ProjectArchitectureProfileVersionEntity entity) {
        try { return normalize(json.readValue(entity.getProfileJson(), ArchitectureProfileDraft.class)); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Unable to read architecture profile", error); }
    }

    private ProjectArchitectureProfileVersionEntity latest(String projectId) {
        return versions.findFirstByProjectIdOrderByRevisionDesc(projectId).orElse(null);
    }

    private void requireProject(String projectId) {
        if (!projects.existsById(projectId)) throw new NotFoundException("Project was not found");
    }

    private ValidationException validation(String code, String targetId, String message) {
        return new ValidationException(List.of(new DependencyDiagnostic("error", code, targetId, targetId, message)));
    }

    private String hash(String value) { return "sha256:" + value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String key(String value) { return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"); }
    private String path(String packageName, String className) { return packageName.replace('.', '/') + "/" + className + ".java"; }
    private String layer(String value, String label) {
        try {
            return ArchitectureLayoutOverrides.from(MigrationProfiles.emptyOverlay())
                    .layer(ArchitectureGraph.ComponentKind.valueOf(value), label);
        } catch (IllegalArgumentException ignored) {
            String key = (value + " " + label).toLowerCase(Locale.ROOT);
            if (key.contains("port")) return "port";
            if (key.contains("controller") || key.contains("inbound")) return "controller";
            if (key.contains("service") || key.contains("use_case") || key.contains("use-case")) return "service";
            if (key.contains("entity") || key.contains("value") || key.contains("model")
                    || key.contains("data-transfer-object")) return "model";
            if (key.contains("repository")) return "repository";
            return key.contains("adapter") || key.contains("outbound") ? "adapter" : "service";
        }
    }
    private String classBase(String value) {
        String compact = value.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (compact.isBlank()) return "GeneratedArtifact";
        StringBuilder result = new StringBuilder();
        for (String part : compact.split("\\s+")) {
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) result.append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return Character.isLetter(result.charAt(0)) ? result.toString() : "Generated" + result;
    }

    public static final class RevisionConflictException extends RuntimeException {
        private final long currentRevision;
        public RevisionConflictException(long currentRevision) {
            super("Expected revision does not match the current Architecture profile revision");
            this.currentRevision = currentRevision;
        }
        public long currentRevision() { return currentRevision; }
    }

    public static final class ValidationException extends RuntimeException {
        private final List<DependencyDiagnostic> diagnostics;
        public ValidationException(List<DependencyDiagnostic> diagnostics) {
            super("Architecture profile validation failed");
            this.diagnostics = List.copyOf(diagnostics);
        }
        public List<DependencyDiagnostic> diagnostics() { return diagnostics; }
    }

    public static final class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }
}
