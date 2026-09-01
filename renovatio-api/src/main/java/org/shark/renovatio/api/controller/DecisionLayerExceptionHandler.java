package org.shark.renovatio.api.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import org.shark.renovatio.api.service.DecisionLayerService;
import org.shark.renovatio.api.service.JpaProfileStore;
import org.shark.renovatio.decisions.DecisionTransitions;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = DecisionLayerController.class)
public class DecisionLayerExceptionHandler {
    @ExceptionHandler(MigrationProfiles.ProfileValidationException.class)
    ResponseEntity<ProfileProblem> validation(MigrationProfiles.ProfileValidationException exception) {
        return ResponseEntity.unprocessableEntity().body(
                new ProfileProblem("PROFILE_VALIDATION_FAILED", exception.violations()));
    }
    @ExceptionHandler(DecisionLayerService.ResourceNotFoundException.class)
    ResponseEntity<ApiProblem> missing() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiProblem("NOT_FOUND", "Resource not found"));
    }
    @ExceptionHandler({JpaProfileStore.ProfileConflictException.class, DecisionTransitions.StaleDecisionException.class})
    ResponseEntity<ApiProblem> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiProblem("REVISION_CONFLICT", "Revision is stale"));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class,
            DecisionTransitions.InvalidOptionException.class,
            IllegalArgumentException.class})
    ResponseEntity<ApiProblem> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiProblem("INVALID_REQUEST", safe(exception.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> unreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        Throwable cause = exception.getMostSpecificCause();
        if (request.getRequestURI().endsWith("/profile") && cause instanceof UnrecognizedPropertyException unknown) {
            return ResponseEntity.unprocessableEntity().body(new ProfileProblem("PROFILE_VALIDATION_FAILED",
                    List.of(new MigrationProfiles.Violation(pointer(unknown), "UNKNOWN_FIELD", "is not allowed"))));
        }
        if (request.getRequestURI().endsWith("/profile") && cause instanceof InvalidFormatException invalid
                && invalid.getTargetType().isEnum()) {
            return ResponseEntity.unprocessableEntity().body(new ProfileProblem("PROFILE_VALIDATION_FAILED",
                    List.of(new MigrationProfiles.Violation(pointer(invalid), "INVALID_ENUM", "is not an allowed value"))));
        }
        return ResponseEntity.badRequest().body(new ApiProblem("INVALID_REQUEST", safe(exception.getMessage())));
    }

    private static String pointer(JsonMappingException exception) {
        String suffix = exception.getPath().stream().map(reference -> reference.getFieldName() == null
                        ? String.valueOf(reference.getIndex()) : reference.getFieldName().replace("~", "~0").replace("/", "~1"))
                .collect(Collectors.joining("/"));
        if (exception instanceof UnrecognizedPropertyException unknown) {
            String property = unknown.getPropertyName().replace("~", "~0").replace("/", "~1");
            if (!suffix.endsWith("/" + property) && !suffix.equals(property)) suffix += "/" + property;
        }
        return "/" + suffix;
    }
    private static String safe(String message) { return message == null || message.isBlank() ? "Invalid request" : message; }
    public record ApiProblem(String code, String message) { }
    public record ProfileProblem(String code, List<MigrationProfiles.Violation> violations) { }
}
