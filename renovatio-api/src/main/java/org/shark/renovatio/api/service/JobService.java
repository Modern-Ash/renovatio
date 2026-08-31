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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

@Service
public class JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepo;
    private final Executor jobExecutor;
    private final SseEventCollector eventCollector;
    private final ObjectMapper objectMapper;
    private final PersistentPlanService planService;
    private final ProjectService projectService;
    private final CobolLanguageProvider cobolLanguageProvider;

    public JobService(JobRepository jobRepo,
                      SseEventCollector eventCollector,
                      ObjectMapper objectMapper,
                      PersistentPlanService planService,
                      ProjectService projectService,
                      CobolLanguageProvider cobolLanguageProvider,
                      @org.springframework.beans.factory.annotation.Qualifier("jobExecutor") Executor jobExecutor) {
        this.jobRepo = jobRepo;
        this.eventCollector = eventCollector;
        this.objectMapper = objectMapper;
        this.planService = planService;
        this.projectService = projectService;
        this.cobolLanguageProvider = cobolLanguageProvider;
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
            eventCollector.send(jobId, "status", Map.of(
                    "status", "COMPLETED",
                    "progress", 1.0,
                    "message", extractCompletionMessage(result),
                    "result", result
            ));
        } catch (Exception e) {
            log.error("Job execution failed: {}", jobId, e);
            entity.setStatus("FAILED");
            entity.setError(e.getMessage());
            eventCollector.send(jobId, "error", Map.of("error", e.getMessage()));
        } finally {
            entity.setCompletedAt(LocalDateTime.now());
            jobRepo.save(entity);
            eventCollector.complete(jobId);
        }
    }

    private Object executeAnalyze(JobEntity entity) {
        Map<String, Object> params = parseParams(entity.getParamsJson());
        String workspacePath = resolveWorkspacePath(entity.getProjectId(), params);
        if (workspacePath == null || workspacePath.isBlank()) {
            throw new IllegalArgumentException("workspacePath is required for analyze jobs");
        }

        long startedAt = System.nanoTime();
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

        log.info("Analyzed COBOL workspace {}: {} source file(s), {} copybook(s), {} program(s) in {} ms (runId={})",
                workspacePath, sourceCount, copybookCount, programCount, elapsedMs, result.getRunId());

        Map<String, Object> summary = Map.of(
                "sourceFiles", sourceCount,
                "copybooks", copybookCount,
                "programs", programCount
        );

        return Map.of(
                "status", "completed",
                "operation", "analyze",
                "runId", result.getRunId(),
                "workspacePath", workspacePath,
                "summary", summary,
                "analysis", result.getData(),
                "message", String.format(
                        "Parsed %d COBOL source file(s) and %d copybook(s) from %s",
                        sourceCount,
                        copybookCount,
                        workspacePath
                ),
                "elapsedMs", elapsedMs,
                "metrics", result.getPerformance() != null ? Map.of("elapsedMs", result.getPerformance().getExecutionTimeMs()) : Map.of()
        );
    }

    private Object executePlan(JobEntity entity) {
        Map<String, Object> params = parseParams(entity.getParamsJson());
        eventCollector.send(entity.getId(), "progress", Map.of("progress", 0.5, "message", "Planning..."));
        return Map.of("status", "completed", "operation", "plan");
    }

    private Object executeApply(JobEntity entity) {
        Map<String, Object> params = parseParams(entity.getParamsJson());
        eventCollector.send(entity.getId(), "progress", Map.of("progress", 0.5, "message", "Applying..."));
        return Map.of("status", "completed", "operation", "apply");
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

    private String resolveWorkspacePath(String projectId, Map<String, Object> params) {
        Object workspaceFromParams = params.get("workspacePath");
        if (workspaceFromParams != null && !workspaceFromParams.toString().isBlank()) {
            return workspaceFromParams.toString();
        }

        Optional<ProjectDto> project = projectService.getProject(projectId);
        return project.map(ProjectDto::getWorkspacePath).orElse(null);
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
