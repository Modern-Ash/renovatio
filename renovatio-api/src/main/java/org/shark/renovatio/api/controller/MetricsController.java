package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.MetricsDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class MetricsController {
    private final ApiAccessService accessService;

    public MetricsController(ApiAccessService accessService) {
        this.accessService = accessService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsDto> getMetrics(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        MetricsDto metrics = MetricsDto.builder()
                .metrics(Map.of())
                .details(Map.of())
                .build();
        return ResponseEntity.ok(metrics);
    }
}
