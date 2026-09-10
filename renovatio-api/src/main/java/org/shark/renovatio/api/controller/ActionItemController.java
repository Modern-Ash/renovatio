package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.ActionItemDto;
import org.shark.renovatio.api.dto.ReviewStatusDto;
import org.shark.renovatio.api.service.ActionItemService;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
public class ActionItemController {
    private final ActionItemService actionItemService;
    private final ApiAccessService accessService;

    public ActionItemController(ActionItemService actionItemService, ApiAccessService accessService) {
        this.actionItemService = actionItemService;
        this.accessService = accessService;
    }

    @GetMapping("/api/projects/{projectId}/action-items")
    public ResponseEntity<List<ActionItemDto>> getActionItems(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(actionItemService.getActionItems(projectId));
    }

    @PostMapping("/api/action-items/{id}/status")
    public ResponseEntity<ActionItemDto> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody ReviewStatusDto statusDto,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canModify(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return actionItemService.updateStatus(id, statusDto.getStatus())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
