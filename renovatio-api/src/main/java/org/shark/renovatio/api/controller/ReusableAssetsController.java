package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.ReusableAssetsService;
import org.shark.renovatio.decisions.DecisionPolicies;
import org.shark.renovatio.decisions.DecisionPolicyCatalog;
import org.shark.renovatio.decisions.PolicyReference;
import org.shark.renovatio.profile.MigrationProfileTemplate;
import org.shark.renovatio.profile.ProfileTemplates;
import org.shark.renovatio.profile.TemplateReference;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class ReusableAssetsController {
    private final ReusableAssetsService service;
    private final ApiAccessService access;

    public ReusableAssetsController(ReusableAssetsService service, ApiAccessService access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping("/api/profile-templates")
    public ResponseEntity<List<ReusableAssetsService.TemplateSummary>> templates(@RequestHeader(value = "X-Role", required = false) String role) {
        return canView(role) ? ResponseEntity.ok(service.templates()) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/api/profile-templates")
    public ResponseEntity<MigrationProfileTemplate> saveTemplate(@RequestHeader(value = "X-Role", required = false) String role,
                                                                  @RequestBody SaveTemplate request) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveTemplate(request.name(), request.version(),
                request.description(), request.projectId()));
    }

    @GetMapping("/api/profile-templates/{name}/versions/{version}")
    public ResponseEntity<MigrationProfileTemplate> template(@RequestHeader(value = "X-Role", required = false) String role,
                                                              @PathVariable String name, @PathVariable String version) {
        return canView(role) ? ResponseEntity.ok(service.template(name, version)) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/api/projects/{projectId}/profile-template")
    public ResponseEntity<MigrationProfileTemplate> bindTemplate(@RequestHeader(value = "X-Role", required = false) String role,
                                                                  @PathVariable String projectId, @RequestBody Reference request) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(service.bindTemplate(projectId, new TemplateReference(request.name(), request.version())));
    }

    @GetMapping("/api/projects/{projectId}/profile-template/diff")
    public ResponseEntity<List<ProfileTemplates.ProfileDiff>> diff(@RequestHeader(value = "X-Role", required = false) String role,
                                                                    @PathVariable String projectId) {
        return canView(role) ? ResponseEntity.ok(service.profileDiff(projectId)) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/api/policy-catalogs")
    public ResponseEntity<List<ReusableAssetsService.PolicySummary>> policies(@RequestHeader(value = "X-Role", required = false) String role) {
        return canView(role) ? ResponseEntity.ok(service.policies()) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/api/policy-catalogs")
    public ResponseEntity<DecisionPolicyCatalog> exportPolicy(@RequestHeader(value = "X-Role", required = false) String role,
                                                               @RequestBody ExportPolicy request) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.exportPolicy(request.name(), request.version(),
                request.projectId(), request.autoConfirmThreshold(), request.suggestThreshold()));
    }

    @GetMapping("/api/policy-catalogs/{name}/versions/{version}")
    public ResponseEntity<DecisionPolicyCatalog> policy(@RequestHeader(value = "X-Role", required = false) String role,
                                                         @PathVariable String name, @PathVariable String version) {
        return canView(role) ? ResponseEntity.ok(service.policy(name, version)) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/api/projects/{projectId}/policy-catalog")
    public ResponseEntity<DecisionPolicies.ApplyReport> bindPolicy(@RequestHeader(value = "X-Role", required = false) String role,
                                                                    @PathVariable String projectId, @RequestBody Reference request) {
        if (!canModify(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(service.bindPolicy(projectId, new PolicyReference(request.name(), request.version())));
    }

    private boolean canView(String role) { return access.canView(AccessRole.fromString(role)); }
    private boolean canModify(String role) { return access.canModify(AccessRole.fromString(role)); }

    public record Reference(String name, String version) { }
    public record SaveTemplate(String name, String version, String projectId, String description) { }
    public record ExportPolicy(String name, String version, String projectId,
                               BigDecimal autoConfirmThreshold, BigDecimal suggestThreshold) { }
}
