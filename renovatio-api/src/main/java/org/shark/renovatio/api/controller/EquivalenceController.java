package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.domain.model.EquivalenceGate;
import org.shark.renovatio.domain.model.EquivalenceComparator;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/equivalence")
public class EquivalenceController {
    private final ApiAccessService access;
    public EquivalenceController(ApiAccessService access) { this.access = access; }
    @PostMapping("/gate")
    public ResponseEntity<EquivalenceGate.Decision> gate(@RequestHeader(value = "X-Role", required = false) String role,
                                                          @RequestBody GateRequest request) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(EquivalenceGate.evaluate(request.totalCases(), request.equivalentCases(), request.minimumRate()));
    }
    public record GateRequest(int totalCases, int equivalentCases, double minimumRate) { }

    @PostMapping("/compare")
    public ResponseEntity<EquivalenceComparator.Comparison> compare(@RequestHeader(value = "X-Role", required = false) String role,
                                                                      @RequestBody CompareRequest request) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(EquivalenceComparator.compare(request.baseline(), request.candidate(), request.ignoredFields()));
    }
    public record CompareRequest(java.util.Map<String, ?> baseline, java.util.Map<String, ?> candidate,
                                 java.util.Set<String> ignoredFields) { }
}
