package org.shark.renovatio.application;

import org.shark.renovatio.application.model.ApplicationModel;
import org.shark.renovatio.application.model.ApplicationModel.*;
import org.shark.renovatio.application.port.ApplicationPorts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic coordinator used by API, CLI and MCP adapters. */
public final class DefaultRenovatioApplication implements RenovatioApplication {
    private final SourceAnalyzer analyzer;
    private final ProposalProvider proposals;
    private final ArchitectureProjector projector;
    private final TargetEmitter emitter;
    private final TargetRefiner refiner;
    private final ValidationGate validator;
    private final ProjectRepository projects;
    private final ArtifactRepository artifacts;
    private final IdempotencyRepository idempotency;
    private final GitPort git;
    private final ClockPort clock;

    public DefaultRenovatioApplication(SourceAnalyzer analyzer, ProposalProvider proposals,
            ArchitectureProjector projector, TargetEmitter emitter, TargetRefiner refiner,
            ValidationGate validator, ProjectRepository projects, ArtifactRepository artifacts,
            IdempotencyRepository idempotency, GitPort git, ClockPort clock) {
        this.analyzer = Objects.requireNonNull(analyzer); this.proposals = Objects.requireNonNull(proposals);
        this.projector = Objects.requireNonNull(projector); this.emitter = Objects.requireNonNull(emitter);
        this.refiner = Objects.requireNonNull(refiner); this.validator = Objects.requireNonNull(validator);
        this.projects = Objects.requireNonNull(projects); this.artifacts = Objects.requireNonNull(artifacts);
        this.idempotency = Objects.requireNonNull(idempotency); this.git = Objects.requireNonNull(git);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override public Project createProject(CreateProject command) {
        requireKey(command.idempotencyKey());
        String digest = ApplicationModel.digest(command.id() + "|" + command.name() + "|" + canonical(command.metadata()));
        return idempotent(command.id(), "create-project", command.idempotencyKey(), digest, Project.class, () -> {
            Project value = new Project(command.id(), command.name(), clock.now(), command.metadata());
            projects.save(value); return value;
        });
    }

    @Override public Analysis analyzeProject(AnalyzeProject query) {
        requireProject(query.projectId());
        SourceSnapshot snapshot = analyzer.snapshot(query.projectId());
        Analysis value = analyzer.analyze(snapshot);
        if (!snapshot.hash().equals(value.sourceHash())) throw new ApplicationFailure.StaleSource("analysis source hash mismatch");
        projects.saveAnalysis(query.projectId(), value); return value;
    }

    @Override public DomainReview reviewDomain(ReviewDomain query) {
        Analysis analysis = projects.analysis(query.projectId()).orElseThrow(() -> new ApplicationFailure.NotFound("analysis not found"));
        DomainReview value = proposals.review(query.projectId(), analysis);
        projects.saveReview(query.projectId(), value); return value;
    }

    @Override public DecisionResolution resolveDecisions(ResolveDecisions command) {
        requireProject(command.projectId()); requireKey(command.idempotencyKey());
        String digest = ApplicationModel.digest(canonical(command.decisions()));
        return idempotent(command.projectId(), "resolve-decisions", command.idempotencyKey(), digest,
                DecisionResolution.class, () -> { projects.saveDecisions(command.projectId(), command.decisions()); return new DecisionResolution(command.decisions()); });
    }

    @Override public MigrationPlan plan(Plan query) {
        Analysis analysis = projects.analysis(query.projectId()).orElseThrow(() -> new ApplicationFailure.NotFound("analysis not found"));
        Map<String, String> decisions = projects.decisions(query.projectId());
        Object projection = projector.project(analysis, decisions);
        String id = ApplicationModel.digest(query.projectId() + "|" + analysis.sourceHash() + "|" + canonical(decisions) + "|" + projection);
        MigrationPlan value = new MigrationPlan(id, query.projectId(), analysis.sourceHash(), projection, decisions);
        projects.savePlan(value); return value;
    }

    @Override public ArtifactManifest preview(Preview query) {
        MigrationPlan plan = projects.plan(query.projectId(), query.planId()).orElseThrow(() -> new ApplicationFailure.NotFound("plan not found"));
        SourceSnapshot current = analyzer.snapshot(query.projectId());
        if (!plan.sourceHash().equals(current.hash())) throw new ApplicationFailure.StaleSource("plan source is stale");
        Map<String, byte[]> emitted = emitter.emit(plan.projection());
        ArtifactManifest value = new ArtifactManifest(null, query.projectId(), plan.sourceHash(), refiner.refine(emitted));
        projects.saveManifest(value); return value;
    }

    @Override public ValidationResult validate(Validate query) {
        ArtifactManifest manifest = manifest(query.projectId(), query.manifestId());
        SourceSnapshot current = analyzer.snapshot(query.projectId());
        if (!manifest.sourceHash().equals(current.hash())) throw new ApplicationFailure.StaleSource("manifest source is stale");
        return validator.validate(manifest);
    }

    @Override public ChangeSet apply(Apply command) {
        requireProject(command.projectId()); requireKey(command.idempotencyKey());
        String digest = ApplicationModel.digest(command.projectId() + "|" + command.manifestId() + "|" + command.expectedSourceHash());
        return idempotent(command.projectId(), "apply", command.idempotencyKey(), digest, ChangeSet.class,
                () -> doApply(command));
    }

    private ChangeSet doApply(Apply command) {
        ArtifactManifest manifest = manifest(command.projectId(), command.manifestId());
        if (!manifest.sourceHash().equals(command.expectedSourceHash())) throw new ApplicationFailure.StaleManifest("unexpected manifest source hash");
        SourceSnapshot current = analyzer.snapshot(command.projectId());
        if (!current.hash().equals(command.expectedSourceHash())) throw new ApplicationFailure.StaleSource("workspace changed after preview");
        ValidationResult validation = validator.validate(manifest);
        if (!validation.valid()) throw new ApplicationFailure.ValidationFailed(String.join("; ", validation.evidence()));
        Map<String, byte[]> preimage = artifacts.read(command.projectId());
        String changeId = ApplicationModel.digest(command.projectId() + "|" + command.manifestId() + "|" + command.idempotencyKey());
        ChangeSet prepared = new ChangeSet(changeId, command.projectId(), manifest.id(), preimage,
                manifest.artifacts(), ChangeState.PREPARED, validation.evidence(), clock.now());
        artifacts.saveChangeSet(prepared);
        String checkpoint = null;
        try {
            artifacts.replace(command.projectId(), manifest.artifacts());
            checkpoint = git.checkpoint(command.projectId(), changeId);
            List<String> evidence = new ArrayList<>(validation.evidence()); evidence.add("git:" + checkpoint);
            ChangeSet applied = prepared.withState(ChangeState.APPLIED, evidence);
            artifacts.saveChangeSet(applied); return applied;
        } catch (RuntimeException failure) {
            try { artifacts.replace(command.projectId(), preimage); } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            if (checkpoint != null) try { git.compensate(command.projectId(), checkpoint); } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            ChangeSet reverted = prepared.withState(ChangeState.REVERTED, List.of("reverted:" + failure.getClass().getSimpleName()));
            artifacts.saveChangeSet(reverted);
            throw new ApplicationFailure.ApplyReverted(changeId, failure);
        }
    }

    @Override public List<ChangeSet> exportEvidence(ExportEvidence query) {
        requireProject(query.projectId()); return List.copyOf(artifacts.history(query.projectId()));
    }

    private ArtifactManifest manifest(String projectId, String id) {
        return projects.manifest(projectId, id).orElseThrow(() -> new ApplicationFailure.StaleManifest("manifest not found"));
    }

    private void requireProject(String projectId) {
        if (projects.find(projectId).isEmpty()) throw new ApplicationFailure.NotFound("project not found: " + projectId);
    }

    private <T> T idempotent(String projectId, String operation, String key, String digest, Class<T> type,
                             java.util.function.Supplier<T> action) {
        var previous = idempotency.find(projectId, operation, key);
        if (previous.isPresent()) {
            IdempotencyRecord record = previous.get();
            if (!record.commandDigest().equals(digest)) throw new ApplicationFailure.IdempotencyConflict("idempotency key reused with different command");
            if (record.successful()) return type.cast(record.result());
        }
        T result = action.get();
        idempotency.save(new IdempotencyRecord(projectId, operation, key, digest, result, true));
        return result;
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("idempotency key is required");
    }

    private static String canonical(Map<?, ?> values) {
        if (values == null) return "{}";
        return new java.util.TreeMap<>(values).toString();
    }
}
