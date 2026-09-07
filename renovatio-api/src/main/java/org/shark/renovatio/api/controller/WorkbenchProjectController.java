package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.WorkbenchAssetDto;
import org.shark.renovatio.api.dto.WorkbenchContextDto;
import org.shark.renovatio.api.dto.WorkbenchAnalysisDto;
import org.shark.renovatio.api.dto.ArchitecturePreviewDto;
import org.shark.renovatio.api.dto.WorkbenchAiDto;
import org.shark.renovatio.api.dto.WorkbenchEquivalenceDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.ProjectService;
import org.shark.renovatio.api.service.WorkbenchProjectAdapterService;
import org.shark.renovatio.api.service.WorkbenchContextService;
import org.shark.renovatio.api.service.WorkbenchAnalysisService;
import org.shark.renovatio.api.service.ArchitecturePreviewService;
import org.shark.renovatio.api.service.WorkbenchAiService;
import org.shark.renovatio.api.service.WorkbenchEquivalenceService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/workbench")
@CrossOrigin(origins = "${renovatio.workbench.allowed-origin:http://127.0.0.1:3000}")
public class WorkbenchProjectController {
    private final ProjectService projects; private final WorkbenchProjectAdapterService adapter; private final WorkbenchContextService context; private final WorkbenchAnalysisService analysis; private final ArchitecturePreviewService architecture; private final WorkbenchAiService ai; private final WorkbenchEquivalenceService equivalence; private final ApiAccessService access;
    @Value("${renovatio.workbench.dev-write-enabled:false}") private boolean devWriteEnabled;
    @Value("${renovatio.workbench.dev-no-auth-enabled:false}") private boolean devNoAuthEnabled;
    public WorkbenchProjectController(ProjectService projects, WorkbenchProjectAdapterService adapter, WorkbenchContextService context, WorkbenchAnalysisService analysis, ArchitecturePreviewService architecture, WorkbenchAiService ai, WorkbenchEquivalenceService equivalence, ApiAccessService access) { this.projects = projects; this.adapter = adapter; this.context = context; this.analysis = analysis; this.architecture = architecture; this.ai = ai; this.equivalence = equivalence; this.access = access; }
    @GetMapping("/assets") public ResponseEntity<List<WorkbenchAssetDto>> assets(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception { if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); return ResponseEntity.ok(adapter.list(root(projectId), devWriteEnabled)); }
    @GetMapping("/assets/{*assetId}") public ResponseEntity<String> read(@PathVariable String projectId, @PathVariable String assetId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { return ResponseEntity.ok(adapter.read(root(projectId), relative(assetId))); }
        catch (SecurityException exception) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }
    }
    @PutMapping("/assets/{*assetId}") public ResponseEntity<Void> write(@PathVariable String projectId, @PathVariable String assetId, @RequestBody String content, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!(devNoAuthEnabled ? devWriteEnabled : access.canModify(AccessRole.fromString(role)))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { adapter.write(root(projectId), relative(assetId), content, devWriteEnabled); return ResponseEntity.noContent().build(); }
        catch (SecurityException exception) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }
    }
    @GetMapping("/context") public ResponseEntity<WorkbenchContextDto> context(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(context.read(projectId));
    }
    @PutMapping("/context") public ResponseEntity<WorkbenchContextDto> saveContext(@PathVariable String projectId, @RequestBody WorkbenchContextDto body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { return ResponseEntity.ok(context.save(projectId, body)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.badRequest().build(); }
    }
    @GetMapping("/analysis") public ResponseEntity<WorkbenchAnalysisDto> analysis(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(analysis.summary(projectId));
    }
    @GetMapping("/architecture") public ResponseEntity<ArchitecturePreviewDto> architecture(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(architecture.preview(projectId, null, null));
    }
    @GetMapping("/ai") public ResponseEntity<WorkbenchAiDto> ai(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(ai.summary(projectId));
    }
    @GetMapping("/equivalence") public ResponseEntity<WorkbenchEquivalenceDto> equivalence(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(equivalence.summary(projectId));
    }
    private Path root(String id) { return projects.getProject(id).map(p -> Path.of(p.getWorkspacePath()).toAbsolutePath().normalize()).orElseThrow(() -> new IllegalArgumentException("Project not found")); }
    private boolean canView(String role) { return devNoAuthEnabled || access.canView(AccessRole.fromString(role)); }
    private String relative(String assetId) { return assetId != null && assetId.startsWith("/") ? assetId.substring(1) : assetId; }
}
