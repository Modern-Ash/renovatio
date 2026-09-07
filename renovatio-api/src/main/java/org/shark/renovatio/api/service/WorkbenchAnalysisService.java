package org.shark.renovatio.api.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.shark.renovatio.api.dto.RunDto;
import org.shark.renovatio.api.dto.WorkbenchAnalysisDto;
import org.shark.renovatio.api.repository.RunSnapshotRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkbenchAnalysisService {
    private final ProjectService projects; private final WorkbenchProjectAdapterService assets; private final RunSnapshotRepository runs;
    public WorkbenchAnalysisService(ProjectService projects, WorkbenchProjectAdapterService assets, RunSnapshotRepository runs) { this.projects = projects; this.assets = assets; this.runs = runs; }
    public WorkbenchAnalysisDto summary(String projectId) throws Exception {
        Path root = projects.getProject(projectId).map(p -> Path.of(p.getWorkspacePath()).toAbsolutePath().normalize()).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        Map<String, Long> inventory = assets.list(root, false).stream().collect(java.util.stream.Collectors.groupingBy(asset -> asset.category(), java.util.TreeMap::new, java.util.stream.Collectors.counting()));
        List<RunDto> history = runs.findByProjectIdOrderByStartedAtDesc(projectId).stream().map(run -> RunDto.builder().runId(run.getRunId()).planId(run.getPlanId()).dryRun(run.getDryRun()).startedAt(run.getStartedAt()).completedAt(run.getCompletedAt()).build()).toList();
        return new WorkbenchAnalysisDto(inventory, history);
    }
}
