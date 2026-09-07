package org.shark.renovatio.api.dto;

import java.util.List;
import java.util.Map;

/** Read-only pre-generation impact report for Theia Shadow/Diff (issue #181). */
public record WorkbenchShadowImpactDto(
        String schemaVersion,
        String canonicalHash,
        Stage source,
        Stage domain,
        Stage architecture,
        List<SourceImpact> sourceImpacts,
        List<ArtifactImpact> artifactImpacts,
        DiffSummary diff,
        Map<String, Object> report) {

    public record Stage(String name, long revision, String hash, int itemCount, String status) { }

    public record SourceImpact(String sourcePath, String kind, int symbolCount,
                               List<String> domainElementIds, List<String> artifactPaths) { }

    public record ArtifactImpact(String path, String role, String layer, String componentId,
                                 String status, String determinism, List<String> sourceRefs,
                                 List<String> domainElementIds, List<String> evidenceRefs) { }

    public record DiffSummary(List<String> plannedArtifacts, List<String> existingTargets,
                              List<String> added, List<String> removed,
                              List<String> changed, List<String> unresolvedEvidence) { }
}
