package org.modernash.renovatio.api.controller;

import org.modernash.renovatio.api.dto.ArchitecturePreviewDto;
import org.modernash.renovatio.api.service.ApiAccessService;
import org.modernash.renovatio.api.service.ArchitecturePreviewService;
import org.modernash.renovatio.profile.MigrationProfile;
import org.modernash.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ArchitecturePreviewController {
    private final ArchitecturePreviewService previews;
    private final ApiAccessService access;

    public ArchitecturePreviewController(ArchitecturePreviewService previews, ApiAccessService access) {
        this.previews = previews;
        this.access = access;
    }

    @GetMapping("/architecture-preview")
    public ResponseEntity<ArchitecturePreviewDto> preview(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(required = false) MigrationProfile.ArchitectureStyle style,
            @RequestParam(required = false) MigrationProfile.ModuleGrouping moduleGrouping) {
        if (!access.canView(AccessRole.fromString(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(previews.preview(projectId, style, moduleGrouping));
    }
}
