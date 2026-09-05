package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.repository.LlmEvaluationRepository;
import org.shark.renovatio.api.entity.LlmEvaluationEntity;
import org.shark.renovatio.llm.eval.LlmEvaluation;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/llm-evaluation")
public class LlmEvaluationController {
    private final ApiAccessService access;
    private final LlmEvaluationRepository repository;
    public LlmEvaluationController(ApiAccessService access, LlmEvaluationRepository repository) { this.access = access; this.repository = repository; }
    @PostMapping("/gate")
    public ResponseEntity<Result> gate(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role,
                                       @RequestBody Request request) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        LlmEvaluation evaluation = new LlmEvaluation(request.datasetId(), request.total(), request.accepted(), request.schemaFailures(), request.provenanceFailures(), request.failures());
        repository.save(new LlmEvaluationEntity(projectId, request.datasetId(), request.total(), request.accepted(), request.schemaFailures(), request.provenanceFailures(), evaluation.acceptanceRate(), evaluation.passes(request.minimumAcceptanceRate())));
        return ResponseEntity.ok(new Result(evaluation, evaluation.passes(request.minimumAcceptanceRate())));
    }
    public record Request(String datasetId, int total, int accepted, int schemaFailures, int provenanceFailures,
                          java.util.List<String> failures, double minimumAcceptanceRate) { }
    public record Result(LlmEvaluation evaluation, boolean passes) { }

    @GetMapping
    public ResponseEntity<?> history(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(repository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(e -> java.util.Map.of(
                "datasetId", e.getDatasetId(), "total", e.getTotal(), "accepted", e.getAccepted(),
                "acceptanceRate", e.getAcceptanceRate(), "passes", e.isPasses(), "createdAt", e.getCreatedAt())).toList());
    }
}
