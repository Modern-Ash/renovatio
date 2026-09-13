package org.shark.renovatio.api.controller;

import java.util.List;
import java.util.Map;
import org.shark.renovatio.api.dto.JobDto;
import org.shark.renovatio.api.dto.JobRequestDto;
import org.shark.renovatio.api.dto.ProjectDto;
import org.shark.renovatio.api.dto.WorkbenchProjectDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.JobService;
import org.shark.renovatio.api.service.ProjectService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "${renovatio.workbench.allowed-origin:http://127.0.0.1:3000}")
public class WorkbenchProjectsController {
    private final ProjectService projects;
    private final JobService jobs;
    private final ApiAccessService access;
    @Value("${renovatio.workbench.dev-no-auth-enabled:false}") private boolean devNoAuthEnabled;

    public WorkbenchProjectsController(ProjectService projects, JobService jobs, ApiAccessService access) {
        this.projects = projects;
        this.jobs = jobs;
        this.access = access;
    }

    @GetMapping("/api/workbench/projects")
    public ResponseEntity<List<WorkbenchProjectDto>> list(@RequestHeader(value = "X-Role", required = false) String role) {
        if (!devNoAuthEnabled && !access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(projects.listProjects().stream().map(this::toWorkbenchProject).toList());
    }

    @GetMapping("/api/workbench/projects/{projectId}")
    public ResponseEntity<WorkbenchProjectDto> get(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!devNoAuthEnabled && !access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return projects.getProject(projectId)
                .map(this::toWorkbenchProject)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/api/workbench/projects/{projectId}/jobs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobDto> createJob(
            @PathVariable String projectId,
            @RequestBody JobRequestDto request,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!devNoAuthEnabled && !access.canCreate(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (request == null || request.getOperation() == null || request.getOperation().isBlank()) return ResponseEntity.badRequest().build();
        JobDto job = jobs.createJob(projectId, request.getOperation(), request.getParams() == null ? Map.of() : request.getParams());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/api/workbench/jobs/{jobId}")
    public ResponseEntity<JobDto> getJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!devNoAuthEnabled && !access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return jobs.getJob(jobId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    private WorkbenchProjectDto toWorkbenchProject(ProjectDto project) {
        return new WorkbenchProjectDto(project.getId(), project.getName(), project.getWorkspacePath(), project.getWorkspacePath());
    }
}
