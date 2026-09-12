package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.WorkbenchAssetDto;
import org.shark.renovatio.api.dto.WorkbenchContextDto;
import org.shark.renovatio.api.dto.WorkbenchAnalysisDto;
import org.shark.renovatio.api.dto.ArchitecturePreviewDto;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto;
import org.shark.renovatio.api.dto.WorkbenchAiDto;
import org.shark.renovatio.api.dto.WorkbenchChangeSetDto;
import org.shark.renovatio.api.dto.WorkbenchDataMigrationDto;
import org.shark.renovatio.api.dto.WorkbenchEquivalenceDto;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto;
import org.shark.renovatio.api.dto.WorkbenchShadowImpactDto;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.ProjectService;
import org.shark.renovatio.api.service.WorkbenchProjectAdapterService;
import org.shark.renovatio.api.service.WorkbenchContextService;
import org.shark.renovatio.api.service.WorkbenchAnalysisService;
import org.shark.renovatio.api.service.ArchitecturePreviewService;
import org.shark.renovatio.api.service.WorkbenchArchitectureCanvasService;
import org.shark.renovatio.api.service.WorkbenchAiService;
import org.shark.renovatio.api.service.WorkbenchChangeSetService;
import org.shark.renovatio.api.service.WorkbenchDataMigrationService;
import org.shark.renovatio.api.service.WorkbenchEquivalenceService;
import org.shark.renovatio.api.service.WorkbenchDomainModelService;
import org.shark.renovatio.api.service.WorkbenchShadowImpactService;
import org.shark.renovatio.api.service.WorkbenchSourceExplorerService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/workbench")
@CrossOrigin(origins = "${renovatio.workbench.allowed-origin:http://127.0.0.1:3000}")
public class WorkbenchProjectController {
    private final ProjectService projects; private final WorkbenchProjectAdapterService adapter; private final WorkbenchContextService context; private final WorkbenchAnalysisService analysis; private final ArchitecturePreviewService architecture; private final WorkbenchArchitectureCanvasService architectureCanvas; private final WorkbenchAiService ai; private final WorkbenchChangeSetService changeSets; private final WorkbenchDataMigrationService dataMigration; private final WorkbenchEquivalenceService equivalence; private final WorkbenchShadowImpactService shadowImpact; private final WorkbenchSourceExplorerService sourceExplorer; private final WorkbenchDomainModelService domainModels; private final ApiAccessService access;
    @Value("${renovatio.workbench.dev-write-enabled:false}") private boolean devWriteEnabled;
    @Value("${renovatio.workbench.dev-no-auth-enabled:false}") private boolean devNoAuthEnabled;
    public WorkbenchProjectController(ProjectService projects, WorkbenchProjectAdapterService adapter, WorkbenchContextService context, WorkbenchAnalysisService analysis, ArchitecturePreviewService architecture, WorkbenchArchitectureCanvasService architectureCanvas, WorkbenchAiService ai, WorkbenchChangeSetService changeSets, WorkbenchDataMigrationService dataMigration, WorkbenchEquivalenceService equivalence, WorkbenchShadowImpactService shadowImpact, WorkbenchSourceExplorerService sourceExplorer, WorkbenchDomainModelService domainModels, ApiAccessService access) { this.projects = projects; this.adapter = adapter; this.context = context; this.analysis = analysis; this.architecture = architecture; this.architectureCanvas = architectureCanvas; this.ai = ai; this.changeSets = changeSets; this.dataMigration = dataMigration; this.equivalence = equivalence; this.shadowImpact = shadowImpact; this.sourceExplorer = sourceExplorer; this.domainModels = domainModels; this.access = access; }
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
    @GetMapping("/architecture/canvas") public ResponseEntity<WorkbenchArchitectureCanvasDto> architectureCanvas(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(architectureCanvas.read(projectId));
    }
    @PutMapping("/architecture/canvas") public ResponseEntity<WorkbenchArchitectureCanvasDto> saveArchitectureCanvas(@PathVariable String projectId, @RequestBody WorkbenchArchitectureCanvasDto.SaveRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(architectureCanvas.save(projectId, body.expectedRevision(), body.profile()));
    }
    @PostMapping("/architecture/canvas:preview") public ResponseEntity<WorkbenchArchitectureCanvasDto> previewArchitectureCanvas(@PathVariable String projectId, @RequestBody WorkbenchArchitectureCanvasDto.PreviewRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(architectureCanvas.preview(projectId, body.profile()));
    }
    @PostMapping("/architecture/canvas:generate") public ResponseEntity<WorkbenchChangeSetDto> generateArchitectureChangeSet(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(changeSets.create(projectId, root(projectId),
                architectureCanvas.generateChangeSetRequest(projectId), actor(role)));
    }
    @GetMapping("/architecture/canvas/versions") public ResponseEntity<List<WorkbenchArchitectureCanvasDto.Version>> architectureVersions(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(architectureCanvas.versions(projectId));
    }
    @PostMapping("/architecture/canvas/versions/{revision}:restore") public ResponseEntity<WorkbenchArchitectureCanvasDto> restoreArchitecture(@PathVariable String projectId, @PathVariable long revision, @RequestBody WorkbenchArchitectureCanvasDto.RestoreRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(architectureCanvas.restore(projectId, revision, body.expectedRevision()));
    }
    @GetMapping("/architecture/canvas/compare") public ResponseEntity<WorkbenchArchitectureCanvasDto.Comparison> compareArchitecture(@PathVariable String projectId, @RequestParam long from, @RequestParam long to, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(architectureCanvas.compare(projectId, from, to));
    }
    @GetMapping("/ai") public ResponseEntity<WorkbenchAiDto> ai(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(ai.summary(projectId));
    }
    @PostMapping("/ai/items/{itemId}:decide") public ResponseEntity<WorkbenchAiDto> decideAiItem(@PathVariable String projectId, @PathVariable String itemId, @RequestBody WorkbenchAiService.DecisionRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(ai.decide(projectId, itemId, body));
    }
    @GetMapping("/data-migration") public ResponseEntity<WorkbenchDataMigrationDto> dataMigration(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(dataMigration.plan(projectId));
    }
    @PostMapping("/data-migration:generate") public ResponseEntity<WorkbenchChangeSetDto> generateDataMigrationChangeSet(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(changeSets.create(projectId, root(projectId),
                dataMigration.generateChangeSetRequest(projectId), actor(role)));
    }
    @GetMapping("/change-sets") public ResponseEntity<List<WorkbenchChangeSetDto>> changeSets(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(changeSets.list(projectId));
    }
    @PostMapping("/change-sets") public ResponseEntity<WorkbenchChangeSetDto> createChangeSet(@PathVariable String projectId, @RequestBody WorkbenchChangeSetDto.CreateRequest body, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(changeSets.create(projectId, root(projectId), body, actor(role)));
    }
    @GetMapping("/change-sets/{changeSetId}") public ResponseEntity<WorkbenchChangeSetDto> changeSet(@PathVariable String projectId, @PathVariable String changeSetId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(changeSets.get(projectId, changeSetId));
    }
    @GetMapping("/change-sets/{changeSetId}/diff") public ResponseEntity<WorkbenchChangeSetDto> changeSetDiff(@PathVariable String projectId, @PathVariable String changeSetId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(changeSets.diff(projectId, changeSetId));
    }
    @PostMapping("/change-sets/{changeSetId}:submit-review") public ResponseEntity<WorkbenchChangeSetDto> submitChangeSet(@PathVariable String projectId, @PathVariable String changeSetId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(changeSets.submitReview(projectId, changeSetId, actor(role)));
    }
    @PostMapping("/change-sets/{changeSetId}:approve") public ResponseEntity<WorkbenchChangeSetDto> approveChangeSet(@PathVariable String projectId, @PathVariable String changeSetId, @RequestBody WorkbenchChangeSetDto.ApprovalRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(changeSets.approve(projectId, changeSetId, body, actor(role)));
    }
    @PostMapping("/change-sets/{changeSetId}:reject") public ResponseEntity<WorkbenchChangeSetDto> rejectChangeSet(@PathVariable String projectId, @PathVariable String changeSetId, @RequestBody(required = false) WorkbenchChangeSetDto.ActionRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(changeSets.reject(projectId, changeSetId, body, actor(role)));
    }
    @PostMapping("/change-sets/{changeSetId}:apply") public ResponseEntity<WorkbenchChangeSetDto> applyChangeSet(@PathVariable String projectId, @PathVariable String changeSetId, @RequestBody WorkbenchChangeSetDto.ActionRequest body, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(changeSets.apply(projectId, changeSetId, root(projectId), body, actor(role), devWriteEnabled));
    }
    @PostMapping("/change-sets/{changeSetId}:rollback") public ResponseEntity<WorkbenchChangeSetDto> rollbackChangeSet(@PathVariable String projectId, @PathVariable String changeSetId, @RequestBody WorkbenchChangeSetDto.ActionRequest body, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(changeSets.rollback(projectId, changeSetId, root(projectId), body, actor(role), devWriteEnabled));
    }
    @GetMapping("/equivalence") public ResponseEntity<WorkbenchEquivalenceDto> equivalence(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(equivalence.summary(projectId));
    }
    @PostMapping("/equivalence/runs") public ResponseEntity<WorkbenchEquivalenceDto.Run> runEquivalence(@PathVariable String projectId, @RequestBody WorkbenchEquivalenceDto.RunRequest body, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.accepted().body(equivalence.run(projectId, body, actor(role)));
    }
    @PostMapping("/equivalence/runs/{runId}:repeat") public ResponseEntity<WorkbenchEquivalenceDto.Run> repeatEquivalence(@PathVariable String projectId, @PathVariable String runId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.accepted().body(equivalence.repeat(projectId, runId, actor(role)));
    }
    @PostMapping("/equivalence/runs/{runId}:cancel") public ResponseEntity<WorkbenchEquivalenceDto.Run> cancelEquivalence(@PathVariable String projectId, @PathVariable String runId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(equivalence.cancel(projectId, runId, actor(role)));
    }
    @PostMapping("/equivalence/runs/{runId}/divergences/{divergenceId}:triage") public ResponseEntity<WorkbenchEquivalenceDto.Run> triageEquivalence(@PathVariable String projectId, @PathVariable String runId, @PathVariable String divergenceId, @RequestBody WorkbenchEquivalenceDto.TriageRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(equivalence.triage(projectId, runId, divergenceId, body, actor(role)));
    }
    @GetMapping("/equivalence/runs/{runId}/report") public ResponseEntity<Map<String, Object>> equivalenceReport(@PathVariable String projectId, @PathVariable String runId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(equivalence.report(projectId, runId));
    }
    @GetMapping("/shadow-impact") public ResponseEntity<WorkbenchShadowImpactDto> shadowImpact(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(shadowImpact.summary(projectId));
    }
    @GetMapping("/source-explorer") public ResponseEntity<WorkbenchSourceExplorerDto> sourceExplorer(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) throws Exception {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(sourceExplorer.summary(projectId));
    }
    @GetMapping("/domain-model") public ResponseEntity<WorkbenchDomainModelDto> domainModel(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(domainModels.read(projectId));
    }
    @PutMapping("/domain-model") public ResponseEntity<WorkbenchDomainModelDto> saveDomainModel(@PathVariable String projectId, @RequestBody WorkbenchDomainModelDto.SaveRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(domainModels.save(projectId, body.expectedRevision(), body.model()));
    }
    @GetMapping("/domain-model/versions") public ResponseEntity<List<WorkbenchDomainModelDto.Version>> domainModelVersions(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(domainModels.versions(projectId));
    }
    @PostMapping("/domain-model/versions/{revision}:restore") public ResponseEntity<WorkbenchDomainModelDto> restoreDomainModel(@PathVariable String projectId, @PathVariable long revision, @RequestBody WorkbenchDomainModelDto.RestoreRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(domainModels.restore(projectId, revision, body.expectedRevision()));
    }
    @GetMapping("/domain-model/compare") public ResponseEntity<WorkbenchDomainModelDto.Comparison> compareDomainModels(@PathVariable String projectId, @RequestParam long from, @RequestParam long to, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canView(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(domainModels.compare(projectId, from, to));
    }
    @PostMapping("/domain-model/suggestions/{suggestionId}") public ResponseEntity<WorkbenchDomainModelDto> decideDomainSuggestion(@PathVariable String projectId, @PathVariable String suggestionId, @RequestBody WorkbenchDomainModelDto.SuggestionRequest body, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(domainModels.decide(projectId, suggestionId, body));
    }
    private Path root(String id) { return projects.getProject(id).map(p -> Path.of(p.getWorkspacePath()).toAbsolutePath().normalize()).orElseThrow(() -> new IllegalArgumentException("Project not found")); }
    private boolean canView(String role) { return devNoAuthEnabled || access.canView(AccessRole.fromString(role)); }
    private boolean canModify(String role) { return devNoAuthEnabled ? devWriteEnabled : access.canModify(AccessRole.fromString(role)); }
    private String actor(String role) { return AccessRole.fromString(role).name().toLowerCase(); }
    private String relative(String assetId) { return assetId != null && assetId.startsWith("/") ? assetId.substring(1) : assetId; }
    @ExceptionHandler(WorkbenchChangeSetService.ChangeSetException.class)
    public ResponseEntity<ErrorResponse> changeSetError(WorkbenchChangeSetService.ChangeSetException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("CHANGE_SET_REJECTED", exception.getMessage()));
    }
    @ExceptionHandler(WorkbenchEquivalenceService.EquivalenceException.class)
    public ResponseEntity<ErrorResponse> equivalenceError(WorkbenchEquivalenceService.EquivalenceException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("EQUIVALENCE_REJECTED", exception.getMessage()));
    }
    @ExceptionHandler(WorkbenchDataMigrationService.DataMigrationException.class)
    public ResponseEntity<ErrorResponse> dataMigrationError(WorkbenchDataMigrationService.DataMigrationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("DATA_MIGRATION_REJECTED", exception.getMessage()));
    }

    public record ErrorResponse(String code, String message) { }
}
