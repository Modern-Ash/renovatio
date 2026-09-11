package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.api.service.ApiAccessService;
import org.shark.renovatio.provider.cobol.pipeline.PipelineRequest;
import org.shark.renovatio.provider.cobol.pipeline.PipelineResult;
import org.shark.renovatio.provider.cobol.service.CobolReferencePipelineService;
import org.shark.renovatio.shared.domain.AccessRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ReferencePipelineController {
    private final ProjectRepository projects;
    private final ApiAccessService access;
    private final CobolReferencePipelineService pipeline;

    public ReferencePipelineController(ProjectRepository projects, ApiAccessService access,
                                       CobolReferencePipelineService pipeline) {
        this.projects = projects;
        this.access = access;
        this.pipeline = pipeline;
    }

    @PostMapping("/reference-pipeline")
    public ResponseEntity<?> execute(
            @PathVariable String projectId,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody(required = false) Request request) {
        if (!access.canView(AccessRole.fromString(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ProjectEntity project = projects.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "PROJECT_NOT_FOUND", "message", "Project not found: " + projectId));
        }

        Path workspace = Path.of(project.getWorkspacePath()).toAbsolutePath().normalize();
        Path output = outputPath(workspace, project, request);
        Path expected = request != null && request.expectedPath() != null && !request.expectedPath().isBlank()
                ? resolve(workspace, request.expectedPath())
                : workspace.resolve("expected");
        boolean verify = request == null || request.verifyEquivalence() == null
                ? true : request.verifyEquivalence();

        PipelineRequest base = PipelineRequest.of(workspace, output);
        PipelineResult result = pipeline.execute(new PipelineRequest(base.fixtureId(), base.fixtureDir(),
                base.decisions(), base.outputDir(), expected, base.deterministic(), verify,
                base.sourceSnapshotHash()));
        return ResponseEntity.status(result.isSuccessful() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY)
                .body(result);
    }

    private static Path outputPath(Path workspace, ProjectEntity project, Request request) {
        if (request != null && request.outputPath() != null && !request.outputPath().isBlank()) {
            return resolve(workspace, request.outputPath());
        }
        String configured = project.getJavaOutputPath();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return workspace.resolve("generated-reference-java").normalize();
    }

    private static Path resolve(Path workspace, String value) {
        Path path = Path.of(value);
        return path.isAbsolute() ? path.normalize() : workspace.resolve(path).normalize();
    }

    public record Request(String outputPath, String expectedPath, Boolean verifyEquivalence) {
    }
}
