package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.DataAccessDto;
import org.shark.renovatio.api.service.DataAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class DataAccessController {

    private final DataAccessService dataAccessService;

    public DataAccessController(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    @GetMapping("/{projectId}/data-accesses")
    public ResponseEntity<Map<String, Object>> getDataAccesses(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        List<DataAccessDto> accesses = dataAccessService.getClassifiedDataAccesses(projectId);
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
