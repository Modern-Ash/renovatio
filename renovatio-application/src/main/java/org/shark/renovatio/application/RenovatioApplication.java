package org.shark.renovatio.application;

import org.shark.renovatio.application.model.ApplicationModel.*;

import java.util.List;

/** The sole transport-neutral application boundary. */
public interface RenovatioApplication {
    Project createProject(CreateProject command);
    Analysis analyzeProject(AnalyzeProject query);
    DomainReview reviewDomain(ReviewDomain query);
    DecisionResolution resolveDecisions(ResolveDecisions command);
    MigrationPlan plan(Plan query);
    ArtifactManifest preview(Preview query);
    ValidationResult validate(Validate query);
    ChangeSet apply(Apply command);
    List<ChangeSet> exportEvidence(ExportEvidence query);
}
