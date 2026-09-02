package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.DataAccessService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class DataAccessController {

    private final DataAccessService dataAccessService;
    private final ApiAccessService access;

    public DataAccessController(DataAccessService dataAccessService, ApiAccessService access) {
        this.dataAccessService = dataAccessService;
        this.access = access;
    }

    @GetMapping("/{projectId}/data-accesses")
    public ResponseEntity<Map<String, Object>> getDataAccesses(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!access.canView(AccessRole.fromString(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<?> accesses = dataAccessService.getClassifiedDataAccesses(projectId);
        if (accesses.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "No data accesses found for project " + projectId
            ));
        }
        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "classifications", accesses
        ));
    }
}
