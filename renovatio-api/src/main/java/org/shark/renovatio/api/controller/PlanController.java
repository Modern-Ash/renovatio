package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.PlanDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.PersistentPlanService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class PlanController {
    private final PersistentPlanService planService;
    private final ApiAccessService accessService;

    public PlanController(PersistentPlanService planService, ApiAccessService accessService) {
        this.planService = planService;
        this.accessService = accessService;
    }

    @GetMapping("/plan")
    public ResponseEntity<PlanDto> getPlan(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
        return planService.getPlan(projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
