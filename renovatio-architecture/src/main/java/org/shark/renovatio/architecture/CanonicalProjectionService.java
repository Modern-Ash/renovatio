package org.shark.renovatio.architecture;

import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.domain.model.SemanticDomainProjector;
import java.util.Objects;

/** Builds the authoritative DomainModel + DecisionSet projection. */
public final class CanonicalProjectionService {
    private final ArchitectureProjector projector;
    public CanonicalProjectionService() { this(new ArchitectureProjector()); }
    public CanonicalProjectionService(ArchitectureProjector projector) { this.projector = Objects.requireNonNull(projector); }

    BaseProjection project(ArchitectureRequest request, ModuleGroupingResolver.GroupingResult grouping) {
        DomainModel domain = new SemanticDomainProjector().project(request.requestHash(), request.programs());
        DecisionSet decisions = new DecisionSet(DecisionSet.SCHEMA_VERSION,
                request.effectiveProfile().profileHash(), request.effectiveProfile().profile().architecture().style(),
                request.effectiveProfile().profile().target().language(), grouping.moduleByProgram(),
                request.effectiveProfile().appliedDecisionIds());
        return new BaseProjection(domain, decisions, projector.project(domain, decisions, request.requestHash()));
    }

    CanonicalProjection complete(BaseProjection base, ArtifactManifest manifest) {
        String hash = ArchitectureSupport.sha256(base.architecture().canonicalHash() + "\n" + manifest.artifacts());
        return new CanonicalProjection(base.domain(), base.decisions(), base.architecture(), manifest, hash);
    }
    record BaseProjection(DomainModel domain, DecisionSet decisions, ArchitectureModel architecture) {}
    public record CanonicalProjection(DomainModel domain, DecisionSet decisions, ArchitectureModel architecture,
                                      ArtifactManifest manifest, String manifestHash) {}
}
