package org.shark.renovatio.api.service;

import org.shark.renovatio.emitter.node.NodeEmitter;
import org.shark.renovatio.emitter.node.DefaultNodeRenderer;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NodePreviewService {
    private final ProjectRepository projects;
    private final JavaGenerationService generation;
    private final DecisionLayerService decisions;

    public NodePreviewService(ProjectRepository projects, JavaGenerationService generation,
                              DecisionLayerService decisions) {
        this.projects = projects;
        this.generation = generation;
        this.decisions = decisions;
    }

    public Map<String, String> generateNodePreview(String projectId) {
        ProjectEntity project = projects.findById(projectId)
                .orElseThrow(() -> new ArchitecturePreviewService.ProjectNotFoundException(projectId));
        Workspace workspace = new Workspace(project.getId(), project.getWorkspacePath(), project.getBranch());
        MigrationProfiles.EffectiveProfile effective = decisions.effective(projectId);
        List<SemanticProgram> programs;
        try {
            programs = generation.semanticPrograms(new NqlQuery(), workspace);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to analyze project workspace: " + projectId, ex);
        }
        NodeEmitter emitter = new NodeEmitter(new DefaultNodeRenderer());
        Map<String, String> files = new LinkedHashMap<>();
        for (SemanticProgram program : programs) {
            TargetModel model = TargetModel.from(program, effective);
            EmittedArtifacts artifacts = emitter.emit(model, model.profile());
            artifacts.utf8TextByPath().forEach((path, content) -> {
                String previous = files.putIfAbsent(path, content);
                if (previous != null && !previous.equals(content)) {
                    throw new IllegalStateException("Conflicting Node preview artifact: " + path);
                }
            });
        }
        return Map.copyOf(files);
    }
}
