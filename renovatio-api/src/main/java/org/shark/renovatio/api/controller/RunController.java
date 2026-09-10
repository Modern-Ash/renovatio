package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.DiffDto;
import org.shark.renovatio.api.dto.RunDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.PersistentPlanService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/runs")
public class RunController {
    private final PersistentPlanService planService;
    private final ApiAccessService accessService;

    public RunController(PersistentPlanService planService, ApiAccessService accessService) {
        this.planService = planService;
        this.accessService = accessService;
    }

    @GetMapping("/{runId}")
    public ResponseEntity<RunDto> getRun(
            @PathVariable String projectId,
            @PathVariable String runId,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return planService.getRun(projectId, runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{runId}/diff")
    public ResponseEntity<DiffDto> getDiff(
            @PathVariable String projectId,
            @PathVariable String runId,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return planService.getRun(projectId, runId)
                .map(run -> DiffDto.builder()
                        .unifiedDiff(run.getDiff())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
