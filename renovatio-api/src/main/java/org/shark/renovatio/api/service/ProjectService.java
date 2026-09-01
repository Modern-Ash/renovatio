package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.ProjectDto;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projectRepo;
    private final DecisionLayerService decisionLayer;

    public ProjectService(ProjectRepository projectRepo, DecisionLayerService decisionLayer) {
        this.projectRepo = projectRepo;
        this.decisionLayer = decisionLayer;
    }

    public ProjectDto createProject(ProjectDto dto) {
        ProjectEntity entity = ProjectEntity.builder()
                .name(dto.getName())
                .workspacePath(dto.getWorkspacePath())
                .branch(dto.getBranch())
                .build();

        entity = projectRepo.save(entity);
        return toDto(entity);
    }

    public Optional<ProjectDto> getProject(String id) {
        return projectRepo.findById(id).map(this::toDto);
    }

    public List<ProjectDto> listProjects() {
        return projectRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteProject(String id) {
        if (!projectRepo.existsById(id)) return;
        decisionLayer.deleteProjectData(id);
        projectRepo.deleteById(id);
    }

    private ProjectDto toDto(ProjectEntity entity) {
        return ProjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .workspacePath(entity.getWorkspacePath())
                .branch(entity.getBranch())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
