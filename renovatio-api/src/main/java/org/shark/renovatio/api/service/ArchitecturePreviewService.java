package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.ArchitecturePreviewDto;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchitecturePreviewService {
    private final ProjectRepository projects;
    private final JavaGenerationService generation;

    public ArchitecturePreviewService(ProjectRepository projects, JavaGenerationService generation) {
        this.projects = projects;
        this.generation = generation;
    }

    @Transactional(readOnly = true)
    public ArchitecturePreviewDto preview(String projectId) {
        ProjectEntity project = projects.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Workspace workspace = new Workspace(project.getId(), project.getWorkspacePath(), project.getBranch());
        return ArchitecturePreviewDto.from(generation.previewArchitecture(new NqlQuery(), workspace));
    }

    public static final class ProjectNotFoundException extends IllegalArgumentException {
        public static final String CODE = "PROJECT_NOT_FOUND";

        public ProjectNotFoundException(String projectId) {
            super("Project not found: " + projectId);
        }
    }
}
