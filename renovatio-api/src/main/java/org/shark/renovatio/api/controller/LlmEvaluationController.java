package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.llm.eval.LlmEvaluation;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/llm-evaluation")
public class LlmEvaluationController {
    private final ApiAccessService access;
    public LlmEvaluationController(ApiAccessService access) { this.access = access; }
    @PostMapping("/gate")
    public ResponseEntity<Result> gate(@RequestHeader(value = "X-Role", required = false) String role,
                                       @RequestBody Request request) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        LlmEvaluation evaluation = new LlmEvaluation(request.datasetId(), request.total(), request.accepted(), request.schemaFailures(), request.provenanceFailures(), request.failures());
        return ResponseEntity.ok(new Result(evaluation, evaluation.passes(request.minimumAcceptanceRate())));
    }
    public record Request(String datasetId, int total, int accepted, int schemaFailures, int provenanceFailures,
                          java.util.List<String> failures, double minimumAcceptanceRate) { }
    public record Result(LlmEvaluation evaluation, boolean passes) { }
}
