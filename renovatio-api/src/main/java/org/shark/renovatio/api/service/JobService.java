package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.api.dto.JobDto;
import org.shark.renovatio.api.entity.JobEntity;
import org.shark.renovatio.api.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public JobService(JobRepository jobRepo,
                      SseEventCollector eventCollector,
                      ObjectMapper objectMapper,
                      PersistentPlanService planService,
                      @org.springframework.beans.factory.annotation.Qualifier("jobExecutor") Executor jobExecutor) {
        this.jobRepo = jobRepo;
        this.eventCollector = eventCollector;
        this.objectMapper = objectMapper;
        this.planService = planService;
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
            eventCollector.send(jobId, "status", Map.of("status", "COMPLETED", "progress", 1.0));
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
        eventCollector.send(entity.getId(), "progress", Map.of("progress", 0.5, "message", "Analyzing..."));
        return Map.of("status", "completed", "operation", "analyze");
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

    public Optional<JobDto> getJob(String jobId) {
        return jobRepo.findById(jobId).map(this::toDto);
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
