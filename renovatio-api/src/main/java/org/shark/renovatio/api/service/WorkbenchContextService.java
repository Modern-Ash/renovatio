package org.shark.renovatio.api.service;

import java.util.Set;
import org.shark.renovatio.api.dto.WorkbenchContextDto;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkbenchContextService {
    private static final Set<String> AREAS = Set.of("project", "analysis", "domain", "architecture", "ai", "equivalence");
    private final ProjectRepository projects;

    public WorkbenchContextService(ProjectRepository projects) { this.projects = projects; }

    public WorkbenchContextDto read(String projectId) {
        ProjectEntity project = project(projectId);
        String area = project.getWorkbenchActiveArea();
        String asset = project.getWorkbenchSelectedAssetId();
        return valid(area, asset) ? new WorkbenchContextDto(area, asset) : new WorkbenchContextDto(null, null);
    }

    @Transactional
    public WorkbenchContextDto save(String projectId, WorkbenchContextDto context) {
        if (context == null || !valid(context.activeArea(), context.selectedAssetId())) {
            throw new IllegalArgumentException("Invalid Workbench context");
        }
        ProjectEntity project = project(projectId);
        project.setWorkbenchActiveArea(context.activeArea());
        project.setWorkbenchSelectedAssetId(context.selectedAssetId());
        projects.save(project);
        return new WorkbenchContextDto(context.activeArea(), context.selectedAssetId());
    }

    private ProjectEntity project(String projectId) { return projects.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Project not found")); }
    private boolean valid(String area, String asset) {
        return area != null && AREAS.contains(area) && asset != null && !asset.isBlank() && !asset.startsWith("/") && !asset.contains("..") && asset.length() <= 512;
    }
}
