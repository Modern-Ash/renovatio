package org.shark.renovatio.api.controller;

import java.util.List;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.Diagnostic;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.ErrorResponse;
import org.shark.renovatio.api.service.WorkbenchDomainModelService.NotFoundException;
import org.shark.renovatio.api.service.WorkbenchDomainModelService.RevisionConflictException;
import org.shark.renovatio.api.service.WorkbenchDomainModelService.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = WorkbenchProjectController.class)
public class WorkbenchDomainModelExceptionHandler {
    @ExceptionHandler(RevisionConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(RevisionConflictException error) {
        Diagnostic diagnostic = new Diagnostic("error", "REVISION_CONFLICT",
                Long.toString(error.currentRevision()), error.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("REVISION_CONFLICT", error.getMessage(), List.of(diagnostic)));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> validation(ValidationException error) {
        return ResponseEntity.badRequest().body(new ErrorResponse("DOMAIN_MODEL_VALIDATION_FAILED",
                error.getMessage(), error.diagnostics()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> missing(NotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DOMAIN_MODEL_NOT_FOUND", error.getMessage(), List.of()));
    }
}
