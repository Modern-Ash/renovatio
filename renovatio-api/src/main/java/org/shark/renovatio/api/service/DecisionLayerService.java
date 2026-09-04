package org.shark.renovatio.api.service;

import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.decisions.DecisionPolicies;
import org.shark.renovatio.decisions.DecisionPolicyRepository;
import org.shark.renovatio.decisions.PolicyReference;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.profile.EffectiveProfileResolver;
import org.shark.renovatio.decisions.DecisionStore;
import org.shark.renovatio.decisions.DecisionTransitions;
import org.shark.renovatio.decisions.F1DecisionCatalog;
import org.shark.renovatio.decisions.ProfileStore;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.profile.ProfileTemplateRepository;
import org.shark.renovatio.profile.TemplateReference;
import org.shark.renovatio.llm.decision.ArchitectureSuggestionGateway;
import org.shark.renovatio.llm.decision.DecisionSuggestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DecisionLayerService implements EffectiveProfileResolver {
    private final ProjectRepository projects;
    private final ProfileStore profiles;
    private final DecisionStore decisions;
    private final ArchitectureSuggestionGateway architectureSuggestions;
    private final ProfileTemplateRepository templates;
    private final DecisionPolicyRepository policies;
    private final DecisionResolver resolver = new DecisionResolver();
    public DecisionLayerService(ProjectRepository projects, ProfileStore profiles, DecisionStore decisions,
                                ArchitectureSuggestionGateway architectureSuggestions,
                                ProfileTemplateRepository templates, DecisionPolicyRepository policies) {
        this.projects = projects; this.profiles = profiles; this.decisions = decisions;
        this.architectureSuggestions = architectureSuggestions;
        this.templates = templates; this.policies = policies;
    }

    public ProfileStore.VersionedProfile profile(String projectId) {
        requireProject(projectId);
        return profiles.find(projectId).orElse(new ProfileStore.VersionedProfile(MigrationProfiles.emptyOverlay(), 0));
    }
    public ProfileStore.VersionedProfile replaceProfile(String projectId, MigrationProfile profile, long revision) {
        requireProject(projectId);
        List<MigrationProfiles.Violation> violations = MigrationProfiles.validateOverlay(profile);
        if (!violations.isEmpty()) throw new MigrationProfiles.ProfileValidationException(violations);
        return profiles.replace(projectId, profile, revision);
    }
    public List<DecisionPoint> decisions(String projectId, DecisionPoint.Category category,
                                         BigDecimal minConfidence, DecisionPoint.Status status) {
        requireProject(projectId);
        if (minConfidence != null && (minConfidence.signum() < 0 || minConfidence.compareTo(BigDecimal.ONE) > 0))
            throw new IllegalArgumentException("minConfidence must be between 0 and 1");
        return decisions.find(projectId, category, minConfidence, status);
    }
    @Transactional
    public DecisionPoint patch(String projectId, String id, String option, long revision) {
        requireProject(projectId);
        DecisionPoint current = decisions.findById(projectId, id).orElseThrow(ResourceNotFoundException::new);
        return decisions.save(projectId, DecisionTransitions.patch(current, option, revision, Instant.now()));
    }
    @Transactional
    public DecisionTransitions.BulkResult bulkConfirm(String projectId, BigDecimal threshold) {
        requireProject(projectId);
        List<DecisionPoint> all = decisions.findAll(projectId);
        DecisionTransitions.BulkResult result = DecisionTransitions.bulkConfirm(all, threshold, Instant.now());
        List<DecisionPoint> saved = decisions.saveAll(projectId, result.items());
        return new DecisionTransitions.BulkResult(saved.size(), result.skipped(), saved);
    }
    public MigrationProfiles.EffectiveProfile effective(String projectId) {
        ProjectEntity project = projects.findById(projectId).orElseThrow(ResourceNotFoundException::new);
        return effectiveFor(project, decisions(projectId, null, null, null));
    }

    private MigrationProfiles.EffectiveProfile effectiveFor(ProjectEntity project, List<DecisionPoint> all) {
        TemplateReference templateReference = project.getProfileTemplateName() == null ? null
                : new TemplateReference(project.getProfileTemplateName(), project.getProfileTemplateVersion());
        PolicyReference policyReference = project.getPolicyCatalogName() == null ? null
                : new PolicyReference(project.getPolicyCatalogName(), project.getPolicyCatalogVersion());
        MigrationProfile template = templateReference == null ? MigrationProfiles.emptyOverlay()
                : templates.find(templateReference).orElseThrow(ResourceNotFoundException::new).profile();
        List<DecisionPoint> inherited = all.stream().filter(value -> value.source() == DecisionPoint.Source.POLICY).toList();
        List<DecisionPoint> local = all.stream().filter(value -> value.source() != DecisionPoint.Source.POLICY).toList();
        return resolver.resolve(template, templateReference, inherited, policyReference,
                profile(project.getId()).profile(), local);
    }

    @Override
    public MigrationProfiles.EffectiveProfile resolve(String projectId) {
        return effective(projectId);
    }

    public void deleteProjectData(String projectId) {
        decisions.deleteProject(projectId);
        profiles.deleteProject(projectId);
    }

    @Transactional
    public AnalysisDecisionSummary upsertAnalysis(String projectId, String semanticIrHash) {
        ProjectEntity project = projects.findById(projectId).orElseThrow(ResourceNotFoundException::new);
        Instant now = Instant.now();
        List<DecisionPoint> generated = F1DecisionCatalog.create(semanticIrHash, now);
        List<DecisionPoint> current = decisions.findAll(projectId);
        List<DecisionPoint> next = new ArrayList<>();
        for (DecisionPoint heuristic : generated) {
            DecisionPoint existing = current.stream().filter(value -> value.id().equals(heuristic.id())).findFirst().orElse(null);
            next.add(existing == null ? heuristic : DecisionTransitions.reconcile(existing, heuristic, now));
        }
        current.stream().filter(value -> generated.stream().noneMatch(item -> item.id().equals(value.id())))
                .map(value -> DecisionTransitions.retire(value, now)).forEach(next::add);
        MigrationProfiles.EffectiveProfile effective = effectiveFor(project, next);
        DecisionSuggestionService.SuggestionBatch suggested = architectureSuggestions.suggest(next,
                effective.profileHash(), effective.profile().llm(), now);
        List<DecisionPoint> finalDecisions = suggested.decisions();
        DecisionPolicies.ApplyReport policyReport = new DecisionPolicies.ApplyReport(0, 0,
                finalDecisions.size(), List.of());
        if (project.getPolicyCatalogName() != null) {
            var reference = new PolicyReference(project.getPolicyCatalogName(), project.getPolicyCatalogVersion());
            var catalog = policies.find(reference).orElseThrow(ResourceNotFoundException::new);
            var applied = DecisionPolicies.apply(catalog, finalDecisions, ReusableAssetsService.ANALYZER_VERSION,
                    java.util.Map.of(), now);
            finalDecisions = applied.decisions();
            policyReport = applied.report();
        }
        decisions.saveAll(projectId, finalDecisions);
        return new AnalysisDecisionSummary(suggested.decisions().size(), suggested.suggestionsAttempted(),
                suggested.suggestionsFailed(), suggested.cacheHits(), policyReport.autoConfirmed(), policyReport.suggested());
    }

    private void requireProject(String projectId) {
        if (!projects.existsById(projectId)) throw new ResourceNotFoundException();
    }
    public record AnalysisDecisionSummary(int total, int suggestionsAttempted,
                                          int suggestionsFailed, int cacheHits,
                                          int policyAutoConfirmed, int policySuggested) { }
    public static final class ResourceNotFoundException extends IllegalArgumentException { }
}
