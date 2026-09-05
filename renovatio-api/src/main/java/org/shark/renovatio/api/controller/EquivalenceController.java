package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.EquivalenceReplayService;
import org.shark.renovatio.domain.model.EquivalenceGate;
import org.shark.renovatio.domain.model.EquivalenceComparator;
import org.shark.renovatio.cobol.ir.replay.SourceReplayRunner;
import org.shark.renovatio.api.entity.ReplayExecutionEntity;
import org.shark.renovatio.api.repository.ReplayExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/equivalence")
public class EquivalenceController {
    private final ApiAccessService access;
    private final EquivalenceReplayService replay;
    private final ReplayExecutionRepository executions;
    private final ObjectMapper mapper;
    public EquivalenceController(ApiAccessService access, EquivalenceReplayService replay, ReplayExecutionRepository executions, ObjectMapper mapper) { this.access = access; this.replay = replay; this.executions = executions; this.mapper = mapper; }
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

    @PostMapping("/replay")
    public ResponseEntity<?> replay(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role, @RequestBody ReplayRequest request) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(replay.replay(projectId, request.caseId(), request.input(), request.ignoredFields()));
    }
    public record ReplayRequest(String caseId, java.util.Map<String, ?> input, java.util.Set<String> ignoredFields) { }

    @PostMapping("/source-replay")
    public ResponseEntity<?> sourceReplay(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role, @RequestBody SourceReplayRequest request) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (request == null || request.caseId() == null || request.caseId().isBlank() || request.source() == null || request.source().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "caseId and source are required"));
        }
        var runner = new SourceReplayRunner(request.source(), request.files(), request.db2Responses());
        org.shark.renovatio.domain.model.ReplayRunner.ReplayResult result;
        try { result = runner.run(new org.shark.renovatio.domain.model.ReplayRunner.ReplayInput(request.caseId(), request.input())); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid replay payload", "detail", e.getMessage())); }
        try { executions.save(new ReplayExecutionEntity(projectId, request.caseId(), mapper.writeValueAsString(result), "SUCCESS".equals(result.status()))); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", "Unable to persist source replay")); }
        return ResponseEntity.ok(result);
    }
    public record SourceReplayRequest(String caseId, String source, java.util.Map<String, ?> input,
                                      java.util.Map<String, java.util.List<java.util.Map<String, ?>>> files,
                                      java.util.Map<String, java.util.Map<String, ?>> db2Responses) { }

    @PostMapping("/source-compare")
    public ResponseEntity<?> sourceCompare(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role, @RequestBody SourceCompareRequest request) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (request == null || request.baselineSource() == null || request.baselineSource().isBlank() || request.candidateSource() == null || request.candidateSource().isBlank()) return ResponseEntity.badRequest().body(java.util.Map.of("error", "baselineSource and candidateSource are required"));
        var input = new org.shark.renovatio.domain.model.ReplayRunner.ReplayInput(request.caseId() == null ? "source-compare" : request.caseId(), request.input());
        var baseline = new SourceReplayRunner(request.baselineSource(), request.files(), request.db2Responses()).run(input);
        var candidate = new SourceReplayRunner(request.candidateSource(), request.files(), request.db2Responses()).run(input);
        var comparison = EquivalenceComparator.compare(baseline.output(), candidate.output(), request.ignoredFields());
        try { executions.save(new ReplayExecutionEntity(projectId, input.caseId(), mapper.writeValueAsString(comparison), comparison.equivalent())); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", "Unable to persist source comparison")); }
        return ResponseEntity.ok(comparison);
    }
    public record SourceCompareRequest(String caseId, String baselineSource, String candidateSource, java.util.Map<String, ?> input,
                                       java.util.Map<String, java.util.List<java.util.Map<String, ?>>> files,
                                       java.util.Map<String, java.util.Map<String, ?>> db2Responses, java.util.Set<String> ignoredFields) { }

    @GetMapping("/history")
    public ResponseEntity<?> history(@PathVariable String projectId, @RequestHeader(value = "X-Role", required = false) String role) {
        if (!access.canView(AccessRole.fromString(role))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(replay.history(projectId));
    }
}
