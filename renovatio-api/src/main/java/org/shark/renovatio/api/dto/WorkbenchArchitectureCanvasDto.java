package org.shark.renovatio.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.shark.renovatio.profile.MigrationProfile;

public record WorkbenchArchitectureCanvasDto(
        long revision,
        String canonicalHash,
        LocalDateTime savedAt,
        ArchitectureProfileDraft profile,
        ArchitecturePreviewDto preview,
        List<CanvasNode> canvas,
        List<DependencyRule> dependencyRules,
        List<DependencyDiagnostic> dependencyDiagnostics,
        List<ManifestEntry> manifest) {

    public record ArchitectureProfileDraft(
            MigrationProfile.ArchitectureStyle style,
            MigrationProfile.ModuleGrouping moduleGrouping,
            MigrationProfile.Framework framework,
            MigrationProfile.PersistenceStrategy persistence,
            Map<String, String> packageRoots,
            Map<String, String> suffixes,
            Map<String, String> classNames,
            List<DependencyRule> dependencyRules,
            Map<String, LayoutPosition> layout,
            List<ExcludedNode> excludedNodeIds) { }

    public record CanvasNode(String id, String layer, String kind, String label,
                             String packageName, String className, String componentId,
                             boolean excluded, String exclusionReason) { }
    public record LayoutPosition(double x, double y) { }
    public record ExcludedNode(String id, String reason) { }
    public record DependencyRule(String fromLayer, String toLayer, boolean allowed, String reason) { }
    public record DependencyDiagnostic(String severity, String code, String fromLayer,
                                       String toLayer, String message) { }
    public record ManifestEntry(String path, String role, String layer, String className,
                                String packageName, String componentId) { }
    public record SaveRequest(long expectedRevision, ArchitectureProfileDraft profile) { }
    public record PreviewRequest(ArchitectureProfileDraft profile) { }
    public record RestoreRequest(long expectedRevision) { }
    public record Version(long revision, String canonicalHash, LocalDateTime savedAt,
                          MigrationProfile.ArchitectureStyle style) { }
    public record Comparison(List<Change> added, List<Change> removed, List<Change> changed) { }
    public record Change(String targetType, String targetId, Object beforeValue, Object afterValue) { }
    public record ErrorResponse(String code, String message, List<DependencyDiagnostic> diagnostics) { }
}
