package org.modernash.renovatio.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.modernash.renovatio.api.dto.RunDto;
import org.modernash.renovatio.api.dto.WorkbenchAnalysisDto;
import org.modernash.renovatio.api.entity.JobEntity;
import org.modernash.renovatio.api.repository.JobRepository;
import org.modernash.renovatio.api.repository.RunSnapshotRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkbenchAnalysisService {
    private final ProjectService projects; private final WorkbenchProjectAdapterService assets; private final RunSnapshotRepository runs; private final JobRepository jobs; private final ObjectMapper objectMapper;
    public WorkbenchAnalysisService(ProjectService projects, WorkbenchProjectAdapterService assets, RunSnapshotRepository runs, JobRepository jobs, ObjectMapper objectMapper) { this.projects = projects; this.assets = assets; this.runs = runs; this.jobs = jobs; this.objectMapper = objectMapper; }
    public WorkbenchAnalysisDto summary(String projectId) throws Exception {
        Path root = projects.getProject(projectId).map(p -> Path.of(p.getWorkspacePath()).toAbsolutePath().normalize()).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        Map<String, Long> assetInventory = assets.list(root, false).stream().collect(java.util.stream.Collectors.groupingBy(asset -> asset.category(), java.util.TreeMap::new, java.util.stream.Collectors.counting()));
        Map<String, Long> inventory = new TreeMap<>(assetInventory);
        mergeLatestAnalyzeSummary(projectId, inventory);
        List<RunDto> history = runs.findByProjectIdOrderByStartedAtDesc(projectId).stream().map(run -> RunDto.builder().runId(run.getRunId()).planId(run.getPlanId()).dryRun(run.getDryRun()).startedAt(run.getStartedAt()).completedAt(run.getCompletedAt()).build()).toList();
        return new WorkbenchAnalysisDto(inventory, history);
    }

    private void mergeLatestAnalyzeSummary(String projectId, Map<String, Long> inventory) {
        jobs.findByProjectIdAndOperationAndStatusOrderByCompletedAtDesc(projectId, "analyze", "COMPLETED").stream()
                .map(JobEntity::getResultJson)
                .filter(result -> result != null && !result.isBlank())
                .findFirst()
                .ifPresent(result -> mergeAnalyzeSummary(result, inventory));
    }

    private void mergeAnalyzeSummary(String resultJson, Map<String, Long> inventory) {
        try {
            JsonNode result = objectMapper.readTree(resultJson);
            JsonNode summary = result.path("summary");
            if (summary.isMissingNode() || summary.isNull()) {
                summary = result.path("analysis").path("summary");
            }
            putPositive(inventory, "sourceFiles", summary.path("sourceFiles").asLong(0));
            putPositive(inventory, "programs", summary.path("programs").asLong(0));
            putPositive(inventory, "copybooks", summary.path("copybooks").asLong(0));
        } catch (Exception ignored) {
            // Workbench inventory should remain available even if an older job has malformed result JSON.
        }
    }

    private void putPositive(Map<String, Long> inventory, String key, long value) {
        if (value > 0) {
            inventory.put(key, value);
        }
    }
}
