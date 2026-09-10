package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.NodePreviewService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class NodePreviewController {

    private final NodePreviewService nodePreviewService;
    private final ApiAccessService access;

    public NodePreviewController(NodePreviewService nodePreviewService, ApiAccessService access) {
        this.nodePreviewService = nodePreviewService;
        this.access = access;
    }

    @GetMapping("/{projectId}/node-preview")
    public ResponseEntity<Map<String, Object>> getNodePreview(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!access.canView(AccessRole.fromString(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Map<String, String> files = nodePreviewService.generateNodePreview(projectId);
        if (files.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "No Node.js preview available for project " + projectId
            ));
        }
        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "files", files
        ));
    }
}
