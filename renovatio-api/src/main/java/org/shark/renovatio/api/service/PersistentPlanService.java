package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.api.dto.PlanDto;
import org.shark.renovatio.api.dto.RunDto;
import org.shark.renovatio.api.entity.MigrationPlanSnapshotEntity;
import org.shark.renovatio.api.entity.RunSnapshotEntity;
import org.shark.renovatio.api.repository.MigrationPlanSnapshotRepository;
import org.shark.renovatio.api.repository.RunSnapshotRepository;
import org.shark.renovatio.application.spi.ApplicationCommandBus;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class PersistentPlanService {
    private final ApplicationCommandBus application;
    private final MigrationPlanSnapshotRepository planRepo;
    private final RunSnapshotRepository runRepo;
    private final ObjectMapper objectMapper;

    public PersistentPlanService(ApplicationCommandBus application,
                                  MigrationPlanSnapshotRepository planRepo,
                                  RunSnapshotRepository runRepo,
                                  ObjectMapper objectMapper) {
        this.application = application;
        this.planRepo = planRepo;
        this.runRepo = runRepo;
        this.objectMapper = objectMapper;
    }

    public PlanResult createPlan(String projectId, NqlQuery query, Scope scope, Workspace workspace) {
        Map<String, Object> arguments = arguments(projectId, query, scope, workspace);
        PlanResult result = convert(application.execute("cobol.plan", arguments), PlanResult.class);
        if (result.isSuccess()) {
            try {
                MigrationPlanSnapshotEntity entity = MigrationPlanSnapshotEntity.builder()
                        .projectId(projectId)
                        .planId(result.getPlanId())
                        .planContentJson(result.getPlanContent())
                        .stepsJson(objectMapper.writeValueAsString(result.getSteps()))
                        .build();
                planRepo.save(entity);
            } catch (Exception e) {
                // Log but don't fail - plan was created successfully
            }
        }
        return result;
    }

    public ApplyResult applyPlan(String projectId, String planId, boolean dryRun, Workspace workspace) {
        Map<String, Object> arguments = arguments(projectId, null, null, workspace);
        arguments.put("planId", planId);
        arguments.put("dryRun", Boolean.toString(dryRun));
        ApplyResult result = convert(application.execute("cobol.apply", arguments), ApplyResult.class);
        if (result.isSuccess()) {
            try {
                RunSnapshotEntity entity = RunSnapshotEntity.builder()
                        .projectId(projectId)
                        .runId(result.getRunId())
                        .planId(planId)
                        .dryRun(dryRun)
                        .diffJson(result.getDiff())
                        .resultJson(objectMapper.writeValueAsString(result))
                        .build();
                runRepo.save(entity);
            } catch (Exception e) {
                // Log but don't fail - apply was successful
            }
        }
        return result;
    }

    public DiffResult generateDiff(String projectId, String runId, Workspace workspace) {
        Map<String, Object> arguments = arguments(projectId, null, null, workspace);
        arguments.put("runId", runId);
        DiffResult result = convert(application.execute("cobol.diff", arguments), DiffResult.class);
        if (result.isSuccess()) {
            try {
                Optional<RunSnapshotEntity> existing = runRepo.findByRunId(runId);
                if (existing.isPresent()) {
                    RunSnapshotEntity entity = existing.get();
                    entity.setDiffJson(result.getUnifiedDiff());
                    runRepo.save(entity);
                }
            } catch (Exception e) {
                // Log but don't fail
            }
        }
        return result;
    }

    public Optional<PlanDto> getPlan(String projectId) {
        return planRepo.findByProjectIdOrderByCreatedAtDesc(projectId)
                .map(entity -> {
                    try {
                        return PlanDto.builder()
                                .planId(entity.getPlanId())
                                .planContent(entity.getPlanContentJson())
                                .build();
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    public Optional<RunDto> getRun(String projectId, String runId) {
        return runRepo.findByProjectIdAndRunId(projectId, runId)
                .map(entity -> RunDto.builder()
                        .runId(entity.getRunId())
                        .planId(entity.getPlanId())
                        .dryRun(entity.getDryRun())
                        .diff(entity.getDiffJson())
                        .startedAt(entity.getStartedAt())
                        .completedAt(entity.getCompletedAt())
                        .build());
    }

    private static Map<String, Object> arguments(String projectId, NqlQuery query, Scope scope, Workspace workspace) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("projectId", projectId);
        if (workspace != null && workspace.getPath() != null) values.put("workspacePath", workspace.getPath());
        if (workspace != null && workspace.getMetadata() != null) {
            Object outputDir = workspace.getMetadata().get("outputDir");
            if (outputDir != null && !outputDir.toString().isBlank()) values.put("outputDir", outputDir.toString());
        }
        if (query != null && query.getOriginalQuery() != null) values.put("nql", query.getOriginalQuery());
        if (scope != null && scope.getIncludePatterns() != null && !scope.getIncludePatterns().isEmpty()) {
            values.put("scope", String.join(",", scope.getIncludePatterns()));
        }
        return values;
    }

    private <T> T convert(Map<String, Object> envelope, Class<T> type) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>(envelope);
        payload.remove("type");
        payload.remove("summary");
        return objectMapper.convertValue(payload, type);
    }
}
