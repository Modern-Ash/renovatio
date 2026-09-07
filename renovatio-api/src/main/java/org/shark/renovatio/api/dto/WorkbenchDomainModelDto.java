package org.shark.renovatio.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.shark.renovatio.domain.model.DomainModel;

public record WorkbenchDomainModelDto(long revision, String canonicalHash, LocalDateTime savedAt,
                                      DomainModel model, List<Diagnostic> diagnostics,
                                      List<Suggestion> suggestions) {
    public record Diagnostic(String severity, String code, String targetId, String message) { }
    public record Suggestion(String id, String targetType, String targetId, String name,
                             String status, Long decidedRevision, LocalDateTime decidedAt) { }
    public record SaveRequest(long expectedRevision, DomainModel model) { }
    public record RestoreRequest(long expectedRevision) { }
    public record Version(long revision, String canonicalHash, LocalDateTime savedAt) { }
    public record Comparison(List<Change> added, List<Change> removed, List<Change> changed) { }
    public record Change(String targetType, String targetId, Object beforeValue, Object afterValue) { }
    public record SuggestionRequest(String action, long expectedRevision,
                                    DomainModel.DomainNode editedNode,
                                    DomainModel.BusinessInvariant editedInvariant) { }
    public record ErrorResponse(String code, String message, List<Diagnostic> diagnostics) { }
}
