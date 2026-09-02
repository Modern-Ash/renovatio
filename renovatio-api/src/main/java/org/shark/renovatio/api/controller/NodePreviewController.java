package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.NodePreviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class NodePreviewController {

    private final NodePreviewService nodePreviewService;

    public NodePreviewController(NodePreviewService nodePreviewService) {
        this.nodePreviewService = nodePreviewService;
    }

    @GetMapping("/{projectId}/node-preview")
    public ResponseEntity<Map<String, Object>> getNodePreview(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role) {
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
