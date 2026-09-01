package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.ProjectDto;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projectRepo;

    public ProjectService(ProjectRepository projectRepo) {
        this.projectRepo = projectRepo;
    }

    public ProjectDto createProject(ProjectDto dto) {
        String workspacePath = dto != null ? dto.getWorkspacePath() : null;
        if (workspacePath == null || workspacePath.isBlank()) {
            throw new IllegalArgumentException("workspacePath is required");
        }

        String normalizedWorkspacePath = normalizeAndCreateWorkspace(workspacePath);
        ProjectEntity entity = ProjectEntity.builder()
                .name(dto.getName())
                .workspacePath(normalizedWorkspacePath)
                .branch(dto.getBranch())
                .javaOutputPath(normalizeJavaOutputPath(dto != null ? dto.getJavaOutputPath() : null, normalizedWorkspacePath))
                .javaPackage(trimOrNull(dto != null ? dto.getJavaPackage() : null))
                .javaArchitecture(trimOrNull(dto != null ? dto.getJavaArchitecture() : null))
                .build();

        entity = projectRepo.save(entity);
        return toDto(entity);
    }

    private String normalizeAndCreateWorkspace(String workspacePath) {
        Path workspace = Paths.get(workspacePath);
        if (!workspace.isAbsolute()) {
            workspace = workspace.toAbsolutePath();
        }
        try {
            Files.createDirectories(workspace);
            return workspace.normalize().toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to create workspace directory: " + workspace, e);
        }
    }

    private String normalizeJavaOutputPath(String javaOutputPath, String workspacePath) {
        if (javaOutputPath == null || javaOutputPath.isBlank()) {
            return Paths.get(workspacePath).resolve("generated-java-stubs").normalize().toString();
        }

        Path requested = Paths.get(javaOutputPath.trim());
        if (requested.isAbsolute()) {
            return requested.normalize().toString();
        }

        return Paths.get(workspacePath).resolve(requested).normalize().toString();
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public Optional<ProjectDto> getProject(String id) {
        return projectRepo.findById(id).map(this::toDto);
    }

    public List<ProjectDto> listProjects() {
        return projectRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ProjectDto toDto(ProjectEntity entity) {
        return ProjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .workspacePath(entity.getWorkspacePath())
                .branch(entity.getBranch())
                .javaOutputPath(entity.getJavaOutputPath())
                .javaPackage(entity.getJavaPackage())
                .javaArchitecture(entity.getJavaArchitecture())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
