package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.service.ArchitecturePreviewService;
import org.shark.renovatio.architecture.ArchitectureTransformer;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice(assignableTypes = ArchitecturePreviewController.class)
public class ArchitecturePreviewExceptionHandler {
    @ExceptionHandler(ArchitecturePreviewService.ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> projectNotFound(ArchitecturePreviewService.ProjectNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ArchitecturePreviewService.ProjectNotFoundException.CODE,
                        error.getMessage(), List.of()));
    }

    @ExceptionHandler(JavaGenerationService.ArchitecturePreviewException.class)
    public ResponseEntity<ErrorResponse> previewFailed(JavaGenerationService.ArchitecturePreviewException error) {
        HttpStatus status = "WORKSPACE_NOT_FOUND".equals(error.code())
                ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(new ErrorResponse(error.code(), error.getMessage(), List.of()));
    }

    @ExceptionHandler(ArchitectureTransformer.ArchitectureStyleNotActiveException.class)
    public ResponseEntity<ErrorResponse> styleNotActive(
            ArchitectureTransformer.ArchitectureStyleNotActiveException error) {
        return ResponseEntity.unprocessableEntity().body(new ErrorResponse(
                ArchitectureTransformer.ArchitectureStyleNotActiveException.CODE,
                error.getMessage(), error.activeStyles().stream().map(Enum::name).toList()));
    }

    public record ErrorResponse(String code, String message, List<String> activeStyles) { }
}
