package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.ProjectDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.ProjectService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final ApiAccessService accessService;

    public ProjectController(ProjectService projectService, ApiAccessService accessService) {
        this.projectService = projectService;
        this.accessService = accessService;
    }

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(
            @RequestBody ProjectDto project,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canCreate(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (project == null || project.getName() == null || project.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ProjectDto created = projectService.createProject(project);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(
            @PathVariable String id,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return projectService.getProject(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> listProjects(
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(projectService.listProjects());
    }
}
