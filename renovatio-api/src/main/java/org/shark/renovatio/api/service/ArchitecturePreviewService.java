package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.ArchitecturePreviewDto;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchitecturePreviewService {
    private final ProjectRepository projects;
    private final JavaGenerationService generation;
    private final DecisionLayerService decisions;

    public ArchitecturePreviewService(ProjectRepository projects, JavaGenerationService generation,
                                      DecisionLayerService decisions) {
        this.projects = projects;
        this.generation = generation;
        this.decisions = decisions;
    }

    @Transactional(readOnly = true)
    public ArchitecturePreviewDto preview(String projectId, MigrationProfile.ArchitectureStyle style,
                                          MigrationProfile.ModuleGrouping moduleGrouping) {
        ProjectEntity project = projects.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Workspace workspace = new Workspace(project.getId(), project.getWorkspacePath(), project.getBranch());
        MigrationProfiles.EffectiveProfile effective = decisions.effective(projectId);
        MigrationProfile.Architecture current = effective.profile().architecture();
        MigrationProfile profile = effective.profile();
        MigrationProfile draft = new MigrationProfile(profile.schemaVersion(), profile.extensions(), profile.target(),
                new MigrationProfile.Architecture(style == null ? current.style() : style,
                        moduleGrouping == null ? current.moduleGrouping() : moduleGrouping),
                profile.runtime(), profile.persistence(), profile.style(), profile.llm());
        MigrationProfiles.EffectiveProfile previewProfile = MigrationProfiles.effective(draft,
                effective.resolvedDecisions(), java.util.Map.of(), effective.appliedDecisionIds());
        return ArchitecturePreviewDto.from(generation.previewArchitecture(new NqlQuery(), workspace, previewProfile));
    }

    public static final class ProjectNotFoundException extends IllegalArgumentException {
        public static final String CODE = "PROJECT_NOT_FOUND";

        public ProjectNotFoundException(String projectId) {
            super("Project not found: " + projectId);
        }
    }
}
