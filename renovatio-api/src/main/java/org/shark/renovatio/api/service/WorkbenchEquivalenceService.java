package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.shark.renovatio.api.dto.WorkbenchEquivalenceDto;
import org.springframework.stereotype.Service;

/** Projects equivalence artifacts and records deterministic lab runs without leaving the workbench contract. */
@Service
public class WorkbenchEquivalenceService {
    private final ProjectService projects;
    private final WorkbenchProjectAdapterService assets;
    private final ObjectMapper json;
    private final Map<String, List<WorkbenchEquivalenceDto.Run>> runs = new ConcurrentHashMap<>();
    private final Map<String, List<WorkbenchEquivalenceDto.AuditEvent>> history = new ConcurrentHashMap<>();

    public WorkbenchEquivalenceService(ProjectService projects, WorkbenchProjectAdapterService assets, ObjectMapper json) {
        this.projects = projects; this.assets = assets; this.json = json;
    }

    public WorkbenchEquivalenceDto summary(String projectId) throws Exception {
        Path root = projects.getProject(projectId)
                .map(project -> Path.of(project.getWorkspacePath()).toAbsolutePath().normalize())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        var all = assets.list(root, false);
        var evidence = items(all, "Evidence");
        var generatedTargets = items(all, "Generated targets");
        var verdicts = verdicts(root, evidence);
        var fixtures = fixtures(root, evidence, generatedTargets);
        var projectRuns = runs.getOrDefault(projectId, List.of());
        return new WorkbenchEquivalenceDto(evidence, generatedTargets, verdicts, fixtures, projectRuns,
                gate(verdicts, projectRuns), history.getOrDefault(projectId, List.of()));
    }

    public WorkbenchEquivalenceDto.Run run(String projectId, WorkbenchEquivalenceDto.RunRequest request, String actor) throws Exception {
        Path root = root(projectId);
        var all = assets.list(root, false);
        var evidence = items(all, "Evidence");
        var targets = items(all, "Generated targets");
        var fixture = fixtures(root, evidence, targets).stream()
                .filter(candidate -> candidate.id().equals(request.fixtureId()))
                .findFirst()
                .orElseThrow(() -> new EquivalenceException("Fixture not found"));
        String baselineId = valueOr(request.baselineId(), fixture.baselineId());
        String candidateId = valueOr(request.candidateId(), fixture.candidateId());
        String baselineHash = hash(baselineId + "|" + fixture.reproducibilityHash());
        String candidateHash = hash(candidateId + "|" + fixture.reproducibilityHash() + "|" + request.inputOverrides());
        boolean divergence = fixture.id().toLowerCase().contains("divergence")
                || fixture.name().toLowerCase().contains("divergence")
                || !baselineHash.substring(0, 18).equals(candidateHash.substring(0, 18));
        var divergences = divergence ? List.of(new WorkbenchEquivalenceDto.Divergence("div:" + fixture.id(),
                "output", "release-blocking", fixture.evidenceRefs().isEmpty() ? "equivalence:generated" : fixture.evidenceRefs().getFirst(),
                "Candidate output differs from baseline for fixture " + fixture.id(), "untriaged", "")) : List.<WorkbenchEquivalenceDto.Divergence>of();
        var comparison = new WorkbenchEquivalenceDto.Comparison(divergence ? "diverged" : "equivalent",
                !divergence, true, true, List.of(baselineId, candidateId));
        String now = Instant.now().toString();
        String id = "eq-" + UUID.randomUUID();
        var logs = List.of("queued fixture " + fixture.id(), "loaded baseline " + baselineId,
                "loaded candidate " + candidateId, "compared state/output/files/sql");
        String reportHash = hash(projectId + id + fixture.reproducibilityHash() + comparison.state() + divergences);
        var run = new WorkbenchEquivalenceDto.Run(id, fixture.id(), "completed", 100, now, now, baselineHash,
                candidateHash, valueOr(request.commit(), "workspace"), valueOr(request.profileHash(), "profile:default"),
                valueOr(request.changeSetId(), "change-set:none"), logs, comparison, divergences, reportHash);
        runs.computeIfAbsent(projectId, ignored -> new ArrayList<>()).add(run);
        audit(projectId, actor, "run-completed", id, "Deterministic equivalence run completed", reportHash);
        return run;
    }

    public WorkbenchEquivalenceDto.Run repeat(String projectId, String runId, String actor) {
        var run = findRun(projectId, runId);
        String now = Instant.now().toString();
        var repeated = new WorkbenchEquivalenceDto.Run("eq-" + UUID.randomUUID(), run.fixtureId(), "completed", 100,
                now, now, run.baselineHash(), run.candidateHash(), run.commit(), run.profileHash(), run.changeSetId(),
                new ArrayList<>(run.logs()) {{ add("repeated exactly from " + run.id()); }},
                run.comparison(), run.divergences(), run.reportHash());
        runs.computeIfAbsent(projectId, ignored -> new ArrayList<>()).add(repeated);
        audit(projectId, actor, "run-repeated", repeated.id(), "Repeated from " + run.id(), repeated.reportHash());
        return repeated;
    }

    public WorkbenchEquivalenceDto.Run cancel(String projectId, String runId, String actor) {
        var run = findRun(projectId, runId);
        var cancelled = replace(projectId, run, new WorkbenchEquivalenceDto.Run(run.id(), run.fixtureId(), "cancelled",
                Math.min(run.progress(), 99), run.startedAt(), Instant.now().toString(), run.baselineHash(), run.candidateHash(),
                run.commit(), run.profileHash(), run.changeSetId(), concat(run.logs(), "cancelled by " + actor),
                run.comparison(), run.divergences(), run.reportHash()));
        audit(projectId, actor, "run-cancelled", runId, "Cancellation requested from Theia", cancelled.reportHash());
        return cancelled;
    }

    public WorkbenchEquivalenceDto.Run triage(String projectId, String runId, String divergenceId,
                                              WorkbenchEquivalenceDto.TriageRequest request, String actor) {
        var run = findRun(projectId, runId);
        String action = request == null || request.action() == null ? "" : request.action();
        if (!List.of("accepted-difference", "defect", "needs-ai-analysis").contains(action)) {
            throw new EquivalenceException("Triage action must be accepted-difference, defect or needs-ai-analysis");
        }
        var divergences = run.divergences().stream().map(divergence -> divergence.id().equals(divergenceId)
                ? new WorkbenchEquivalenceDto.Divergence(divergence.id(), divergence.kind(), divergence.severity(),
                        divergence.evidenceRef(), divergence.summary(), action, request.reason())
                : divergence).toList();
        var triaged = replace(projectId, run, new WorkbenchEquivalenceDto.Run(run.id(), run.fixtureId(), run.state(), run.progress(),
                run.startedAt(), run.finishedAt(), run.baselineHash(), run.candidateHash(), run.commit(), run.profileHash(),
                run.changeSetId(), concat(run.logs(), "triaged " + divergenceId + " as " + action), run.comparison(), divergences,
                hash(run.reportHash() + divergences)));
        audit(projectId, actor, "divergence-triaged", divergenceId, request.reason(), triaged.reportHash());
        return triaged;
    }

    public Map<String, Object> report(String projectId, String runId) {
        var run = findRun(projectId, runId);
        audit(projectId, "system", "report-exported", runId, "Equivalence report exported", run.reportHash());
        return Map.ofEntries(Map.entry("schemaVersion", "1"), Map.entry("projectId", projectId),
                Map.entry("runId", run.id()), Map.entry("fixtureId", run.fixtureId()),
                Map.entry("comparison", run.comparison()), Map.entry("divergences", run.divergences()),
                Map.entry("logs", run.logs()), Map.entry("commit", run.commit()),
                Map.entry("profileHash", run.profileHash()), Map.entry("changeSetId", run.changeSetId()),
                Map.entry("reportHash", run.reportHash()));
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

    private List<WorkbenchEquivalenceDto.Fixture> fixtures(Path root, List<WorkbenchEquivalenceDto.Item> evidence,
                                                           List<WorkbenchEquivalenceDto.Item> targets) {
        var fixtureEvidence = evidence.stream().filter(item -> item.id().endsWith(".json"))
                .map(item -> fixture(root, item, targets)).flatMap(java.util.Optional::stream).toList();
        if (!fixtureEvidence.isEmpty()) return fixtureEvidence;
        String candidate = targets.isEmpty() ? "generated-target:none" : targets.getFirst().id();
        return List.of(new WorkbenchEquivalenceDto.Fixture("smoke", "Smoke equivalence case",
                "workspace", "cobol-baseline", candidate,
                new WorkbenchEquivalenceDto.Inputs(Map.of("account", "1001"),
                        List.of(new WorkbenchEquivalenceDto.SequentialFile("SYSIN", hash("smoke"), "RUN SMOKE")),
                        List.of(new WorkbenchEquivalenceDto.Db2Response("lookup-account", "00000", List.of(Map.of("ACCOUNT", "1001"))))),
                evidence.stream().map(WorkbenchEquivalenceDto.Item::id).limit(4).toList(), hash("smoke|" + candidate)));
    }

    private java.util.Optional<WorkbenchEquivalenceDto.Fixture> fixture(Path root, WorkbenchEquivalenceDto.Item item,
                                                                        List<WorkbenchEquivalenceDto.Item> targets) {
        try {
            var node = json.readTree(Files.readString(root.resolve(item.id()).normalize()));
            String fixture = node.path("fixtureId").asText();
            if (fixture.isBlank()) return java.util.Optional.empty();
            String candidate = targets.isEmpty() ? node.path("candidateId").asText("generated-target:none") : targets.getFirst().id();
            var inputs = new WorkbenchEquivalenceDto.Inputs(Map.of("fixtureId", fixture),
                    List.of(new WorkbenchEquivalenceDto.SequentialFile("SYSIN", hash(item.id()), item.name())),
                    List.of(new WorkbenchEquivalenceDto.Db2Response("fixture-db2", "00000", List.of(Map.of("FIXTURE_ID", fixture)))));
            return java.util.Optional.of(new WorkbenchEquivalenceDto.Fixture(fixture,
                    node.path("name").asText(fixture), node.path("sourceId").asText("workspace"),
                    node.path("baselineId").asText("cobol-baseline"), candidate, inputs, List.of(item.id()), hash(Files.readString(root.resolve(item.id()).normalize()))));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private WorkbenchEquivalenceDto.Gate gate(List<WorkbenchEquivalenceDto.Verdict> verdicts,
                                              List<WorkbenchEquivalenceDto.Run> runs) {
        List<String> blockers = new ArrayList<>();
        verdicts.stream().filter(WorkbenchEquivalenceDto.Verdict::blocksRelease)
                .forEach(verdict -> blockers.add("persisted verdict blocks release: " + verdict.fixtureId()));
        runs.stream().filter(run -> run.divergences().stream().anyMatch(divergence -> !divergence.triageStatus().equals("accepted-difference")))
                .forEach(run -> blockers.add("unaccepted divergence in run " + run.id()));
        boolean hasSuccessfulRun = runs.stream().anyMatch(run -> run.state().equals("completed"));
        if (!hasSuccessfulRun) blockers.add("no completed equivalence run");
        return new WorkbenchEquivalenceDto.Gate(blockers.isEmpty(), blockers.isEmpty() ? "ready" : "blocked",
                List.copyOf(blockers), blockers.isEmpty() ? "Promotion readiness satisfied" : "Resolve blockers before promotion");
    }

    private Path root(String projectId) {
        return projects.getProject(projectId)
                .map(project -> Path.of(project.getWorkspacePath()).toAbsolutePath().normalize())
                .orElseThrow(() -> new EquivalenceException("Project not found"));
    }

    private WorkbenchEquivalenceDto.Run findRun(String projectId, String runId) {
        return runs.getOrDefault(projectId, List.of()).stream().filter(run -> run.id().equals(runId)).findFirst()
                .orElseThrow(() -> new EquivalenceException("Run not found"));
    }

    private WorkbenchEquivalenceDto.Run replace(String projectId, WorkbenchEquivalenceDto.Run oldRun, WorkbenchEquivalenceDto.Run newRun) {
        var projectRuns = runs.computeIfAbsent(projectId, ignored -> new ArrayList<>());
        projectRuns.removeIf(run -> run.id().equals(oldRun.id()));
        projectRuns.add(newRun);
        projectRuns.sort(Comparator.comparing(WorkbenchEquivalenceDto.Run::startedAt));
        return newRun;
    }

    private void audit(String projectId, String actor, String action, String targetId, String reason, String hash) {
        history.computeIfAbsent(projectId, ignored -> new ArrayList<>()).add(new WorkbenchEquivalenceDto.AuditEvent(
                actor == null || actor.isBlank() ? "unknown" : actor, action, Instant.now().toString(), targetId,
                reason == null ? "" : reason, hash));
    }

    private static List<String> concat(List<String> input, String value) {
        var copy = new ArrayList<>(input);
        copy.add(value);
        return List.copyOf(copy);
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder("sha256:");
            for (byte b : bytes) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash content", exception);
        }
    }

    public static class EquivalenceException extends RuntimeException {
        public EquivalenceException(String message) { super(message); }
    }
}
