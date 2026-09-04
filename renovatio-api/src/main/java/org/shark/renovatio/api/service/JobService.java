package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.api.dto.ProjectDto;
import org.shark.renovatio.api.dto.JobDto;
import org.shark.renovatio.api.entity.JobEntity;
import org.shark.renovatio.api.repository.JobRepository;
import org.shark.renovatio.provider.cobol.CobolLanguageProvider;
import org.shark.renovatio.shared.domain.AnalyzeResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Service
public class JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private static final String KEY_BROWSER_WORKSPACE = "browserWorkspace";
    private static final String KEY_WORKSPACE_LABEL = "workspaceLabel";

    private final JobRepository jobRepo;
    private final Executor jobExecutor;
    private final SseEventCollector eventCollector;
    private final ObjectMapper objectMapper;
    private final PersistentPlanService planService;
    private final ProjectService projectService;
    private final CobolLanguageProvider cobolLanguageProvider;
    private final DecisionLayerService decisionLayerService;
    private final DataAccessService dataAccessService;

    public JobService(JobRepository jobRepo,
                      SseEventCollector eventCollector,
                      ObjectMapper objectMapper,
                      PersistentPlanService planService,
                      ProjectService projectService,
                      CobolLanguageProvider cobolLanguageProvider,
                      DecisionLayerService decisionLayerService,
                      DataAccessService dataAccessService,
                      @org.springframework.beans.factory.annotation.Qualifier("jobExecutor") Executor jobExecutor) {
        this.jobRepo = jobRepo;
        this.eventCollector = eventCollector;
        this.objectMapper = objectMapper;
        this.planService = planService;
        this.projectService = projectService;
        this.cobolLanguageProvider = cobolLanguageProvider;
        this.decisionLayerService = decisionLayerService;
        this.dataAccessService = dataAccessService;
        this.jobExecutor = jobExecutor;
    }

    public JobDto createJob(String projectId, String operation, Map<String, Object> params) {
        try {
            JobEntity entity = JobEntity.builder()
                    .projectId(projectId)
                    .operation(operation)
                    .status("PENDING")
                    .paramsJson(params != null ? objectMapper.writeValueAsString(params) : "{}")
                    .build();

            entity = jobRepo.save(entity);
            final String jobId = entity.getId();
            jobExecutor.execute(() -> executeJob(jobId));

            return toDto(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create job", e);
        }
    }

    public JobDto createBrowserAnalyzeJob(String projectId, List<MultipartFile> files, String workspaceLabel) {
        Path browserWorkspaceRoot = null;
        try {
            if (files == null || files.isEmpty()) {
                throw new IllegalArgumentException("No files were uploaded");
            }

            browserWorkspaceRoot = Files.createTempDirectory("renovatio-browser-workspace-");
            int writtenFiles = writeUploadedWorkspace(browserWorkspaceRoot, files);
            if (writtenFiles == 0) {
                throw new IllegalArgumentException("No files were uploaded");
            }

            Map<String, Object> params = new java.util.LinkedHashMap<>();
            params.put("workspacePath", browserWorkspaceRoot.toString());
            params.put(KEY_BROWSER_WORKSPACE, true);
            if (workspaceLabel != null && !workspaceLabel.isBlank()) {
                params.put(KEY_WORKSPACE_LABEL, workspaceLabel);
            }
            params.put("uploadedFiles", writtenFiles);
            return createJob(projectId, "analyze", params);
        } catch (IOException e) {
            cleanupWorkspace(browserWorkspaceRoot);
            throw new RuntimeException("Failed to prepare browser workspace", e);
        } catch (RuntimeException e) {
            cleanupWorkspace(browserWorkspaceRoot);
            throw e;
        }
    }

    private void executeJob(String jobId) {
        JobEntity entity = jobRepo.findById(jobId).orElse(null);
        if (entity == null) {
            log.error("Job not found: {}", jobId);
            return;
        }

        entity.setStatus("RUNNING");
        entity.setStartedAt(LocalDateTime.now());
        jobRepo.save(entity);
        eventCollector.send(jobId, "status", Map.of("status", "RUNNING", "progress", 0.0));

        try {
            Object result = switch (entity.getOperation()) {
                case "analyze" -> executeAnalyze(entity);
                case "plan" -> executePlan(entity);
                case "apply" -> executeApply(entity);
                case "diff" -> executeDiff(entity);
                default -> throw new IllegalArgumentException("Unknown operation: " + entity.getOperation());
            };

            entity.setStatus("COMPLETED");
            entity.setResultJson(objectMapper.writeValueAsString(result));
            entity.setProgress(1.0);
            Map<String, Object> completionEvent = new java.util.LinkedHashMap<>();
            completionEvent.put("status", "COMPLETED");
            completionEvent.put("progress", 1.0);
            String completionMessage = extractCompletionMessage(result);
            if (completionMessage != null && !completionMessage.isBlank()) {
                completionEvent.put("message", completionMessage);
            }
            completionEvent.put("result", result);
            eventCollector.send(jobId, "status", completionEvent);
        } catch (Exception e) {
            log.error("Job execution failed: {}", jobId, e);
            entity.setStatus("FAILED");
            entity.setError(e.getMessage());
            eventCollector.send(jobId, "error", Map.of("error", String.valueOf(e.getMessage())));
        } finally {
            entity.setCompletedAt(LocalDateTime.now());
            jobRepo.save(entity);
            cleanupBrowserWorkspace(entity);
            eventCollector.complete(jobId);
        }
    }

    private Object executeAnalyze(JobEntity entity) {
        Map<String, Object> params = parseParams(entity.getParamsJson());
        String workspacePath = resolveWorkspacePath(entity.getProjectId(), params);
        if (workspacePath == null || workspacePath.isBlank()) {
            throw new IllegalArgumentException("workspacePath is required for analyze jobs");
        }

        Path workspaceRoot = Paths.get(workspacePath);
        if (!Files.isDirectory(workspaceRoot)) {
            throw new IllegalArgumentException("Workspace directory not found or inaccessible: " + workspacePath);
        }

        long startedAt = System.nanoTime();
        String workspaceDisplay = workspaceDisplayName(params, workspacePath);
        eventCollector.send(entity.getId(), "progress", Map.of(
                "progress", 0.1,
                "message", "Validating COBOL workspace..."
        ));

        Workspace workspace = new Workspace(entity.getProjectId(), workspacePath, null);
        workspace.setMetadata(new java.util.LinkedHashMap<>(params));

        NqlQuery query = new NqlQuery();
        query.setLanguage("cobol");
        query.setParameters(params);

        eventCollector.send(entity.getId(), "progress", Map.of(
                "progress", 0.35,
                "message", "Scanning workspace for COBOL files..."
        ));

        AnalyzeResult result = cobolLanguageProvider.analyze(query, workspace);
        if (!result.isSuccess()) {
          throw new IllegalStateException(result.getMessage());
        }

        int sourceCount = extractListCount(result, "sourceFiles");
        int copybookCount = extractListCount(result, "copybooks");
        int programCount = extractProgramCount(result);
        long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        eventCollector.send(entity.getId(), "progress", Map.of(
                "progress", 0.9,
                "message", String.format(
                        "Parsed %d COBOL source file(s) and %d copybook(s)",
                        sourceCount,
                        copybookCount
                )
        ));

        eventCollector.send(entity.getId(), "progress", Map.of(
                "progress", 0.95,
                "message", "Finalizing analysis..."
        ));

        log.info("Analyzed COBOL workspace {}: {} source file(s), {} copybook(s), {} program(s) in {} ms (runId={})",
                workspacePath, sourceCount, copybookCount, programCount, elapsedMs, result.getRunId());

        Map<String, Object> summary = Map.of(
                "sourceFiles", sourceCount,
                "copybooks", copybookCount,
                "programs", programCount
        );
        String semanticIrHash = org.shark.renovatio.profile.MigrationProfiles.sha256(
                org.shark.renovatio.profile.MigrationProfiles.canonical(result.getData()));
        DecisionLayerService.AnalysisDecisionSummary decisionSummary =
                decisionLayerService.upsertAnalysis(entity.getProjectId(), semanticIrHash);

        List<org.shark.renovatio.semantic.ir.SemanticProgram> semanticPrograms;
        try {
            semanticPrograms = cobolLanguageProvider.semanticPrograms(query, workspace);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to project semantic programs for data-access classification",
                    exception);
        }
        List<org.shark.renovatio.api.dto.DataAccessDto> dataAccesses =
                dataAccessService.classifyFromPrograms(semanticPrograms,
                        decisionLayerService.effective(entity.getProjectId()));

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("status", "completed");
        response.put("operation", "analyze");
        if (result.getRunId() != null && !result.getRunId().isBlank()) {
            response.put("runId", result.getRunId());
        }
        response.put("workspacePath", workspaceDisplay);
        response.put("workspaceResolvedPath", workspacePath);
        response.put("summary", summary);
        response.put("analysis", result.getData());
        response.put("decisions", decisionSummary);
        response.put("dataAccesses", dataAccesses);
        response.put(
                "message",
                String.format(
                        "Parsed %d COBOL source file(s) and %d copybook(s) from %s",
                        sourceCount,
                        copybookCount,
                        workspaceDisplay
                )
        );
        response.put("elapsedMs", elapsedMs);
        if (result.getPerformance() != null) {
            Map<String, Object> metrics = new java.util.LinkedHashMap<>();
            metrics.put("elapsedMs", result.getPerformance().getExecutionTimeMs());
            response.put("metrics", metrics);
        } else {
            response.put("metrics", Map.of());
        }
        return response;
    }

    private Object executePlan(JobEntity entity) {
        Map<String, Object> params = parseParams(entity.getParamsJson());
        var effective = requireActiveTarget(entity.getProjectId());
        eventCollector.send(entity.getId(), "progress", Map.of("progress", 0.5, "message", "Planning..."));
        return Map.of("status", "completed", "operation", "plan", "profileHash", effective.profileHash());
    }

    private Object executeApply(JobEntity entity) {
        Map<String, Object> params = parseParams(entity.getParamsJson());
        var effective = requireActiveTarget(entity.getProjectId());
        List<Map<String, Object>> planSteps = extractPlanSteps(params.get("planSteps"));
        eventCollector.send(entity.getId(), "progress", Map.of("progress", 0.5, "message", "Building dry run preview..."));
        eventCollector.send(entity.getId(), "progress", Map.of("progress", 0.9, "message", "Finalizing dry run preview..."));

        List<String> enabledSteps = new ArrayList<>();
        List<String> disabledSteps = new ArrayList<>();
        for (Map<String, Object> step : planSteps) {
            String label = String.valueOf(step.getOrDefault("description", step.getOrDefault("type", "Unknown step")));
            boolean enabled = Boolean.parseBoolean(String.valueOf(step.getOrDefault("enabled", Boolean.FALSE)));
            if (enabled) {
                enabledSteps.add(label);
            } else {
                disabledSteps.add(label);
            }
        }

        StringBuilder preview = new StringBuilder();
        preview.append("Dry run preview\n");
        preview.append("Selected migration steps:\n");
        if (enabledSteps.isEmpty()) {
            preview.append("- No migration steps were enabled\n");
        } else {
            for (String step : enabledSteps) {
                preview.append("- ").append(step).append("\n");
            }
        }
        if (!disabledSteps.isEmpty()) {
            preview.append("\nSkipped steps:\n");
            for (String step : disabledSteps) {
                preview.append("- ").append(step).append("\n");
            }
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("status", "completed");
        result.put("operation", "apply");
        result.put("profileHash", effective.profileHash());
        result.put("dryRun", Boolean.parseBoolean(String.valueOf(params.getOrDefault("dryRun", Boolean.TRUE))));
        result.put("message", "Dry run completed successfully!");
        result.put("preview", preview.toString().trim());
        result.put("selectedSteps", enabledSteps);
        result.put("skippedSteps", disabledSteps);
        result.put("planSteps", planSteps);
        Map<String, Object> changes = new java.util.LinkedHashMap<>();
        changes.put("enabledSteps", enabledSteps.size());
        changes.put("skippedSteps", disabledSteps.size());
        changes.put("dryRun", Boolean.TRUE);
        result.put("changes", changes);
        return result;
    }

    private org.shark.renovatio.profile.MigrationProfiles.EffectiveProfile requireActiveTarget(String projectId) {
        var effective = decisionLayerService.effective(projectId);
        if (effective.profile().target().language() != org.shark.renovatio.profile.MigrationProfile.Language.JAVA) {
            throw new IllegalStateException("TARGET_NOT_ACTIVE");
        }
        return effective;
    }

    private Object executeDiff(JobEntity entity) {
        Map<String, Object> params = parseParams(entity.getParamsJson());
        eventCollector.send(entity.getId(), "progress", Map.of("progress", 0.5, "message", "Generating diff..."));
        return Map.of("status", "completed", "operation", "diff");
    }

    private Map<String, Object> parseParams(String paramsJson) {
        try {
            return objectMapper.readValue(paramsJson, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractPlanSteps(Object rawPlanSteps) {
        if (!(rawPlanSteps instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> steps = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> step = new java.util.LinkedHashMap<>();
                map.forEach((key, value) -> step.put(String.valueOf(key), value));
                steps.add(step);
            }
        }
        return steps;
    }

    private String resolveWorkspacePath(String projectId, Map<String, Object> params) {
        Object workspaceFromParams = params.get("workspacePath");
        if (workspaceFromParams != null && !workspaceFromParams.toString().isBlank()) {
            return workspaceFromParams.toString();
        }

        Optional<ProjectDto> project = projectService.getProject(projectId);
        return project.map(ProjectDto::getWorkspacePath).orElse(null);
    }

    private String workspaceDisplayName(Map<String, Object> params, String fallbackWorkspacePath) {
        Object workspaceLabel = params.get(KEY_WORKSPACE_LABEL);
        if (workspaceLabel != null && !workspaceLabel.toString().isBlank()) {
            return workspaceLabel.toString();
        }
        return fallbackWorkspacePath;
    }

    private int writeUploadedWorkspace(Path targetRoot, List<MultipartFile> files) throws IOException {
        int writtenFiles = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String uploadedName = file.getOriginalFilename();
            if (uploadedName == null || uploadedName.isBlank()) {
                uploadedName = file.getName();
            }

            Path relativePath = Paths.get(uploadedName).normalize();
            if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
                throw new IllegalArgumentException("Invalid uploaded file path: " + uploadedName);
            }

            Path target = targetRoot.resolve(relativePath).normalize();
            if (!target.startsWith(targetRoot)) {
                throw new IllegalArgumentException("Invalid uploaded file path: " + uploadedName);
            }

            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            writtenFiles++;
        }
        return writtenFiles;
    }

    private void cleanupBrowserWorkspace(JobEntity entity) {
        if (entity == null) {
            return;
        }
        Map<String, Object> params = parseParams(entity.getParamsJson());
        Object browserWorkspace = params.get(KEY_BROWSER_WORKSPACE);
        Object workspacePath = params.get("workspacePath");
        if (!(browserWorkspace instanceof Boolean isBrowserWorkspace) || !isBrowserWorkspace) {
            return;
        }
        if (workspacePath == null || workspacePath.toString().isBlank()) {
            return;
        }
        cleanupWorkspace(Paths.get(workspacePath.toString()));
    }

    private void cleanupWorkspace(Path root) {
        if (root == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }

    private int extractProgramCount(AnalyzeResult result) {
        if (result == null || result.getData() == null) {
            return 0;
        }

        Object programs = result.getData().get("programs");
        if (programs instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }

    private int extractListCount(AnalyzeResult result, String key) {
        if (result == null || result.getData() == null) {
            return 0;
        }

        Object value = result.getData().get(key);
        if (value instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }

    private String extractCompletionMessage(Object result) {
        if (result instanceof Map<?, ?> map) {
            Object message = map.get("message");
            if (message != null && !message.toString().isBlank()) {
                return message.toString();
            }
        }
        return null;
    }

    public Optional<JobDto> getJob(String jobId) {
        return jobRepo.findById(jobId).map(this::toDto);
    }

    public List<JobDto> listJobsForProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return List.of();
        }
        return jobRepo.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<JobDto> listRecentJobs() {
        return jobRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    private JobDto toDto(JobEntity entity) {
        try {
            return JobDto.builder()
                    .id(entity.getId())
                    .projectId(entity.getProjectId())
                    .operation(entity.getOperation())
                    .status(entity.getStatus())
                    .progress(entity.getProgress())
                    .result(entity.getResultJson() != null ? objectMapper.readValue(entity.getResultJson(), Object.class) : null)
                    .error(entity.getError())
                    .createdAt(entity.getCreatedAt())
                    .startedAt(entity.getStartedAt())
                    .completedAt(entity.getCompletedAt())
                    .build();
        } catch (Exception e) {
            return JobDto.builder()
                    .id(entity.getId())
                    .projectId(entity.getProjectId())
                    .operation(entity.getOperation())
                    .status(entity.getStatus())
                    .progress(entity.getProgress())
                    .error(entity.getError())
                    .createdAt(entity.getCreatedAt())
                    .startedAt(entity.getStartedAt())
                    .completedAt(entity.getCompletedAt())
                    .build();
        }
    }
}
