package org.shark.renovatio.api.controller;

import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto;
import org.shark.renovatio.api.service.WorkbenchArchitectureCanvasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = WorkbenchProjectController.class)
public class WorkbenchArchitectureCanvasExceptionHandler {
    @ExceptionHandler(WorkbenchArchitectureCanvasService.RevisionConflictException.class)
    ResponseEntity<WorkbenchArchitectureCanvasDto.ErrorResponse> conflict(
            WorkbenchArchitectureCanvasService.RevisionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new WorkbenchArchitectureCanvasDto.ErrorResponse(
                "REVISION_CONFLICT", exception.getMessage(), java.util.List.of()));
    }

    @ExceptionHandler(WorkbenchArchitectureCanvasService.ValidationException.class)
    ResponseEntity<WorkbenchArchitectureCanvasDto.ErrorResponse> validation(
            WorkbenchArchitectureCanvasService.ValidationException exception) {
        return ResponseEntity.unprocessableEntity().body(new WorkbenchArchitectureCanvasDto.ErrorResponse(
                "ARCHITECTURE_PROFILE_VALIDATION_FAILED", exception.getMessage(), exception.diagnostics()));
    }

    @ExceptionHandler(WorkbenchArchitectureCanvasService.NotFoundException.class)
    ResponseEntity<WorkbenchArchitectureCanvasDto.ErrorResponse> notFound(
            WorkbenchArchitectureCanvasService.NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new WorkbenchArchitectureCanvasDto.ErrorResponse(
                "NOT_FOUND", exception.getMessage(), java.util.List.of()));
    }
}
