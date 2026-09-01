package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.DecisionLayerService;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.DecisionTransitions;
import org.shark.renovatio.decisions.ProfileStore;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class DecisionLayerController {
    private final DecisionLayerService service;
    private final ApiAccessService access;
    public DecisionLayerController(DecisionLayerService service, ApiAccessService access) {
        this.service = service; this.access = access;
    }

    @GetMapping("/profile")
    public ResponseEntity<MigrationProfile> getProfile(@PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        ProfileStore.VersionedProfile result = service.profile(projectId);
        return ResponseEntity.ok().eTag("\"" + result.revision() + "\"").body(result.profile());
    }

    @PutMapping("/profile")
    public ResponseEntity<MigrationProfile> putProfile(@PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody MigrationProfile profile) {
        if (!access.canModify(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        ProfileStore.VersionedProfile result = service.replaceProfile(projectId, profile, parseEtag(ifMatch));
        return ResponseEntity.ok().eTag("\"" + result.revision() + "\"").body(result.profile());
    }

    @GetMapping("/profile:effective")
    public ResponseEntity<?> effective(@PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(service.effective(projectId));
    }

    @GetMapping("/decisions")
    public ResponseEntity<DecisionList> decisions(@PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(required = false) DecisionPoint.Category category,
            @RequestParam(required = false) BigDecimal minConfidence,
            @RequestParam(required = false) DecisionPoint.Status status) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        List<DecisionPoint> items = service.decisions(projectId, category, minConfidence, status);
        return ResponseEntity.ok(new DecisionList(items, items.size()));
    }

    @PatchMapping("/decisions/{decisionId}")
    public ResponseEntity<DecisionPoint> patch(@PathVariable String projectId, @PathVariable String decisionId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody PatchDecision request) {
        if (!access.canModify(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(service.patch(projectId, decisionId, request.chosenOption(), request.revision()));
    }

    @PostMapping("/decisions:bulk-confirm")
    public ResponseEntity<DecisionTransitions.BulkResult> bulk(@PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody BulkConfirm request) {
        if (!access.canModify(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(service.bulkConfirm(projectId, request.minConfidence()));
    }

    static long parseEtag(String value) {
        if (value == null || !value.matches("\"[0-9]+\"")) throw new IllegalArgumentException("If-Match must be a quoted decimal ETag");
        return Long.parseLong(value.substring(1, value.length() - 1));
    }

    public record DecisionList(List<DecisionPoint> items, int total) { }
    public record PatchDecision(String chosenOption, long revision) { }
    public record BulkConfirm(BigDecimal minConfidence) { }
}
