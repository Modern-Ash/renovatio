package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.api.dto.PlanDto;
import org.shark.renovatio.api.dto.RunDto;
import org.shark.renovatio.api.entity.MigrationPlanSnapshotEntity;
import org.shark.renovatio.api.entity.RunSnapshotEntity;
import org.shark.renovatio.api.repository.MigrationPlanSnapshotRepository;
import org.shark.renovatio.api.repository.RunSnapshotRepository;
import org.shark.renovatio.provider.cobol.service.MigrationPlanService;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class PersistentPlanService {
    private final MigrationPlanService planService;
    private final MigrationPlanSnapshotRepository planRepo;
    private final RunSnapshotRepository runRepo;
    private final ObjectMapper objectMapper;

    public PersistentPlanService(MigrationPlanService planService,
                                  MigrationPlanSnapshotRepository planRepo,
                                  RunSnapshotRepository runRepo,
                                  ObjectMapper objectMapper) {
        this.planService = planService;
        this.planRepo = planRepo;
        this.runRepo = runRepo;
        this.objectMapper = objectMapper;
    }

    public PlanResult createPlan(String projectId, NqlQuery query, Scope scope, Workspace workspace) {
        PlanResult result = planService.createMigrationPlan(query, scope, workspace);
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
        ApplyResult result = planService.applyMigrationPlan(planId, dryRun, workspace);
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
        DiffResult result = planService.generateDiff(runId, workspace);
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
}
