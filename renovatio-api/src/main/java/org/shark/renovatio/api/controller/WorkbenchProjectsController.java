package org.shark.renovatio.api.controller;

import java.util.List;
import org.shark.renovatio.api.dto.WorkbenchProjectDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.ProjectService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workbench/projects")
@CrossOrigin(origins = "${renovatio.workbench.allowed-origin:http://127.0.0.1:3000}")
public class WorkbenchProjectsController {
    private final ProjectService projects;
    private final ApiAccessService access;
    @Value("${renovatio.workbench.dev-no-auth-enabled:false}") private boolean devNoAuthEnabled;

    public WorkbenchProjectsController(ProjectService projects, ApiAccessService access) { this.projects = projects; this.access = access; }

    @GetMapping
    public ResponseEntity<List<WorkbenchProjectDto>> list(@RequestHeader(value = "X-Role", required = false) String role) {
        if (!devNoAuthEnabled && !access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(projects.listProjects().stream().map(p -> new WorkbenchProjectDto(p.getId(), p.getName())).toList());
    }
}
