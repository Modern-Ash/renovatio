package org.shark.renovatio.application;

import org.shark.renovatio.application.model.ApplicationModel.*;
import org.shark.renovatio.application.port.ApplicationPorts.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reusable in-memory adapters for application and external-adapter contract suites. */
public final class ApplicationContractTestKit {
    public final MemoryProjects projects = new MemoryProjects();
    public final MemoryArtifacts artifacts = new MemoryArtifacts();
    public final MemoryIdempotency idempotency = new MemoryIdempotency();
    public final MutableAnalyzer analyzer = new MutableAnalyzer();
    public final RecordingGit git = new RecordingGit();
    public int emitCalls;
    public int refineCalls;
    public int validateCalls;

    public DefaultRenovatioApplication application() {
        ProposalProvider proposals = (project, analysis) -> new DomainReview(analysis.semanticModel(), List.of("proposal:" + project));
        ArchitectureProjector projector = (analysis, decisions) -> analysis.semanticModel() + ":" + new java.util.TreeMap<>(decisions);
        TargetEmitter emitter = projection -> { emitCalls++; return bytes("generated.txt", projection.toString()); };
        TargetRefiner refiner = generated -> { refineCalls++; return generated; };
        ValidationGate validator = manifest -> { validateCalls++; return new ValidationResult(true, List.of("validated:" + manifest.id())); };
        return new DefaultRenovatioApplication(analyzer, proposals, projector, emitter, refiner, validator,
                projects, artifacts, idempotency, git, () -> Instant.parse("2026-09-09T00:00:00Z"));
    }

    public void seed(DefaultRenovatioApplication app) {
        app.createProject(new CreateProject("project", "Project", "create-1", Map.of()));
        app.analyzeProject(new AnalyzeProject("project"));
        app.resolveDecisions(new ResolveDecisions("project", "decisions-1", Map.of("style", "hexagonal")));
    }

    public static Map<String, byte[]> bytes(String path, String value) {
        return Map.of(path, value.getBytes(StandardCharsets.UTF_8));
    }

    public final class MutableAnalyzer implements SourceAnalyzer {
        Map<String, byte[]> source = bytes("source.cob", "IDENTIFICATION DIVISION.");
        @Override public SourceSnapshot snapshot(String projectId) { return new SourceSnapshot(projectId, null, source); }
        @Override public Analysis analyze(SourceSnapshot snapshot) { return new Analysis(snapshot.hash(), "semantic", List.of("parsed")); }
    }

    public static final class MemoryProjects implements ProjectRepository {
        final Map<String, Project> projects = new HashMap<>(); final Map<String, Analysis> analyses = new HashMap<>();
        final Map<String, DomainReview> reviews = new HashMap<>(); final Map<String, Map<String, String>> decisions = new HashMap<>();
        final Map<String, MigrationPlan> plans = new HashMap<>(); final Map<String, ArtifactManifest> manifests = new HashMap<>();
        @Override public Optional<Project> find(String id) { return Optional.ofNullable(projects.get(id)); }
        @Override public void save(Project value) { projects.put(value.id(), value); }
        @Override public Optional<Analysis> analysis(String id) { return Optional.ofNullable(analyses.get(id)); }
        @Override public void saveAnalysis(String id, Analysis value) { analyses.put(id, value); }
        @Override public Optional<DomainReview> review(String id) { return Optional.ofNullable(reviews.get(id)); }
        @Override public void saveReview(String id, DomainReview value) { reviews.put(id, value); }
        @Override public Map<String, String> decisions(String id) { return decisions.getOrDefault(id, Map.of()); }
        @Override public void saveDecisions(String id, Map<String, String> value) { decisions.put(id, Map.copyOf(value)); }
        @Override public Optional<MigrationPlan> plan(String id, String planId) { return Optional.ofNullable(plans.get(id + ":" + planId)); }
        @Override public void savePlan(MigrationPlan value) { plans.put(value.projectId() + ":" + value.id(), value); }
        @Override public Optional<ArtifactManifest> manifest(String id, String manifestId) { return Optional.ofNullable(manifests.get(id + ":" + manifestId)); }
        @Override public void saveManifest(ArtifactManifest value) { manifests.put(value.projectId() + ":" + value.id(), value); }
    }

    public static final class MemoryArtifacts implements ArtifactRepository {
        Map<String, byte[]> workspace = Map.of(); final List<ChangeSet> history = new ArrayList<>();
        boolean failReplace;
        @Override public Map<String, byte[]> read(String projectId) { return copy(workspace); }
        @Override public void replace(String projectId, Map<String, byte[]> value) {
            if (failReplace) { failReplace = false; throw new IllegalStateException("injected write failure"); }
            workspace = copy(value);
        }
        @Override public void saveChangeSet(ChangeSet value) { history.add(value); }
        @Override public List<ChangeSet> history(String projectId) { return history.stream().filter(v -> v.projectId().equals(projectId)).toList(); }
        private static Map<String, byte[]> copy(Map<String, byte[]> value) {
            Map<String, byte[]> copy = new LinkedHashMap<>(); value.forEach((key, bytes) -> copy.put(key, bytes.clone())); return copy;
        }
    }

    public static final class MemoryIdempotency implements IdempotencyRepository {
        final Map<String, IdempotencyRecord> values = new HashMap<>();
        private String key(String project, String operation, String key) { return project + ":" + operation + ":" + key; }
        @Override public Optional<IdempotencyRecord> find(String project, String operation, String key) { return Optional.ofNullable(values.get(key(project, operation, key))); }
        @Override public void save(IdempotencyRecord value) { values.put(key(value.projectId(), value.operation(), value.key()), value); }
    }

    public static final class RecordingGit implements GitPort {
        int checkpoints; int compensations; boolean fail;
        @Override public String checkpoint(String project, String changeSet) {
            checkpoints++; if (fail) throw new IllegalStateException("injected git failure"); return "checkpoint-" + checkpoints;
        }
        @Override public void compensate(String project, String checkpoint) { compensations++; }
    }
}
