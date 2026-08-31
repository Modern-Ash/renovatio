package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.JobDto;
import org.shark.renovatio.api.dto.JobRequestDto;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.api.service.JobService;
import org.shark.renovatio.api.service.SseEventCollector;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
public class JobController {
    private final JobService jobService;
    private final ApiAccessService accessService;
    private final SseEventCollector eventCollector;

    public JobController(JobService jobService, ApiAccessService accessService, SseEventCollector eventCollector) {
        this.jobService = jobService;
        this.accessService = accessService;
        this.eventCollector = eventCollector;
    }

    @PostMapping("/api/projects/{projectId}/jobs")
    public ResponseEntity<JobDto> createJob(
            @PathVariable String projectId,
            @Valid @RequestBody JobRequestDto request,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canCreate(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        JobDto job = jobService.createJob(projectId, request.getOperation(), request.getParams());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/api/jobs/{jobId}")
    public ResponseEntity<JobDto> getJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return jobService.getJob(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/api/jobs/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToJobEvents(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Role", required = false) String roleHeader) {
        AccessRole role = AccessRole.fromString(roleHeader);
        if (!accessService.canView(role)) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("Forbidden"));
            return emitter;
        }
        return eventCollector.subscribe(jobId);
    }
}
