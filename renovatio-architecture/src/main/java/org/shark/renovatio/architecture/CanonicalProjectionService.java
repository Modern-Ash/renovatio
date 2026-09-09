package org.shark.renovatio.architecture;

import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.domain.model.SemanticDomainProjector;
import java.util.Objects;

/** Builds the single canonical projection shared by preview and apply. */
public final class CanonicalProjectionService {
    private final ArchitectureProjector projector;
    public CanonicalProjectionService() { this(new ArchitectureProjector()); }
    public CanonicalProjectionService(ArchitectureProjector projector) {
        this.projector = Objects.requireNonNull(projector, "projector");
    }

    CanonicalProjection project(ArchitectureRequest request, ModuleGroupingResolver.GroupingResult grouping,
                                ArchitectureGraph graph, ArtifactManifest manifest) {
        DomainModel domain = new SemanticDomainProjector().project(request.requestHash(), request.programs());
        DecisionSet decisions = new DecisionSet(DecisionSet.SCHEMA_VERSION,
                request.effectiveProfile().profileHash(), request.effectiveProfile().profile().architecture().style(),
                request.effectiveProfile().profile().target().language(), grouping.moduleByProgram(),
                request.effectiveProfile().appliedDecisionIds());
        ArchitectureModel architecture = projector.project(domain, decisions, request.requestHash(), graph);
        String manifestHash = ArchitectureSupport.sha256(architecture.canonicalHash() + "\n" + manifest.artifacts());
        return new CanonicalProjection(domain, decisions, architecture, manifest, manifestHash);
    }

    public record CanonicalProjection(DomainModel domain, DecisionSet decisions,
                                      ArchitectureModel architecture, ArtifactManifest manifest,
                                      String manifestHash) {
        public CanonicalProjection {
            Objects.requireNonNull(domain, "domain"); Objects.requireNonNull(decisions, "decisions");
            Objects.requireNonNull(architecture, "architecture"); Objects.requireNonNull(manifest, "manifest");
            manifestHash = ArchitectureSupport.hash(manifestHash, "manifestHash");
        }
    }
}
