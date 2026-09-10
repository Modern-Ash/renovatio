package org.shark.renovatio.api.service;

import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.decisions.DecisionPolicies;
import org.shark.renovatio.decisions.DecisionPolicyCatalog;
import org.shark.renovatio.decisions.DecisionPolicyRepository;
import org.shark.renovatio.decisions.DecisionStore;
import org.shark.renovatio.decisions.PolicyReference;
import org.shark.renovatio.decisions.ProfileStore;
import org.shark.renovatio.profile.MigrationProfileTemplate;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.profile.ProfileTemplateRepository;
import org.shark.renovatio.profile.ProfileTemplates;
import org.shark.renovatio.profile.TemplateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ReusableAssetsService {
    public static final String ANALYZER_VERSION = "renovatio-f8-v1";

    private final ProjectRepository projects;
    private final ProfileStore profiles;
    private final DecisionStore decisions;
    private final ProfileTemplateRepository templates;
    private final DecisionPolicyRepository policies;

    public ReusableAssetsService(ProjectRepository projects, ProfileStore profiles, DecisionStore decisions,
                                 ProfileTemplateRepository templates, DecisionPolicyRepository policies) {
        this.projects = projects;
        this.profiles = profiles;
        this.decisions = decisions;
        this.templates = templates;
        this.policies = policies;
    }

    public List<TemplateSummary> templates() {
        return templates.list().stream().map(value -> new TemplateSummary(value.name(), value.version(),
                value.description(), value.contentHash(), value.createdAt(), usage(value.name(), value.version(), true))).toList();
    }

    public MigrationProfileTemplate template(String name, String version) {
        return templates.find(new TemplateReference(name, version)).orElseThrow(ResourceNotFoundException::new);
    }

    public MigrationProfileTemplate saveTemplate(String name, String version, String description, String projectId) {
        requireProject(projectId);
        var profile = profiles.find(projectId).map(ProfileStore.VersionedProfile::profile)
                .orElse(MigrationProfiles.emptyOverlay());
        return templates.save(ProfileTemplates.snapshot(name, version, description, profile, Instant.now()));
    }

    @Transactional
    public MigrationProfileTemplate bindTemplate(String projectId, TemplateReference reference) {
        ProjectEntity project = requireProject(projectId);
        MigrationProfileTemplate template = templates.find(reference).orElseThrow(ResourceNotFoundException::new);
        project.setProfileTemplateName(reference.name());
        project.setProfileTemplateVersion(reference.version());
        projects.save(project);
        return template;
    }

    public List<ProfileTemplates.ProfileDiff> profileDiff(String projectId) {
        ProjectEntity project = requireProject(projectId);
        if (project.getProfileTemplateName() == null) return List.of();
        var template = template(project.getProfileTemplateName(), project.getProfileTemplateVersion());
        var overlay = profiles.find(projectId).map(ProfileStore.VersionedProfile::profile)
                .orElse(MigrationProfiles.emptyOverlay());
        return ProfileTemplates.diff(template, overlay);
    }

    public List<PolicySummary> policies() {
        return policies.list().stream().map(value -> new PolicySummary(value.name(), value.version(),
                value.contentHash(), value.createdAt(), value.entries().size(),
                usage(value.name(), value.version(), false))).toList();
    }

    public DecisionPolicyCatalog policy(String name, String version) {
        return policies.find(new PolicyReference(name, version)).orElseThrow(ResourceNotFoundException::new);
    }

    public DecisionPolicyCatalog exportPolicy(String name, String version, String projectId,
                                              BigDecimal autoThreshold, BigDecimal suggestThreshold) {
        requireProject(projectId);
        var catalog = DecisionPolicies.exportCatalog(name, version, projectId, ANALYZER_VERSION,
                decisions.findAll(projectId), Map.of(), autoThreshold, suggestThreshold, Instant.now());
        return policies.save(catalog);
    }

    @Transactional
    public DecisionPolicies.ApplyReport bindPolicy(String projectId, PolicyReference reference) {
        ProjectEntity project = requireProject(projectId);
        DecisionPolicyCatalog catalog = policies.find(reference).orElseThrow(ResourceNotFoundException::new);
        var result = DecisionPolicies.apply(catalog, decisions.findAll(projectId), ANALYZER_VERSION, Map.of(), Instant.now());
        decisions.saveAll(projectId, result.decisions());
        project.setPolicyCatalogName(reference.name());
        project.setPolicyCatalogVersion(reference.version());
        projects.save(project);
        return result.report();
    }

    private List<String> usage(String name, String version, boolean template) {
        return projects.findAll().stream().filter(project -> template
                        ? name.equals(project.getProfileTemplateName()) && version.equals(project.getProfileTemplateVersion())
                        : name.equals(project.getPolicyCatalogName()) && version.equals(project.getPolicyCatalogVersion()))
                .map(ProjectEntity::getId).sorted().toList();
    }

    private ProjectEntity requireProject(String projectId) {
        return projects.findById(projectId).orElseThrow(ResourceNotFoundException::new);
    }

    public record TemplateSummary(String name, String version, String description, String contentHash,
                                  Instant createdAt, List<String> projects) { }
    public record PolicySummary(String name, String version, String contentHash, Instant createdAt,
                                int entries, List<String> projects) { }
    public static final class ResourceNotFoundException extends IllegalArgumentException { }
}
