package org.shark.renovatio.api.service;

import org.shark.renovatio.emitter.node.NodeEmitter;
import org.shark.renovatio.emitter.node.DefaultNodeRenderer;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NodePreviewService {

    public Map<String, String> generateNodePreview(String projectId) {
        SourceSpan span = new SourceSpan("src/program.cob", 1, 1, 1, 9);
        SourceProvenance provenance = new SourceProvenance("src/program.cob", "0".repeat(64),
                "COBOL", Optional.empty(), List.of());
        SemanticProgram program = new SemanticProgram("1",
                SemanticProgram.Header.create(projectId, SemanticProgram.NodeKind.PROGRAM, "program", span),
                projectId, provenance, List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
        MigrationProfile profile = MigrationProfiles.emptyOverlay();
        TargetModel model = TargetModel.from(program, MigrationProfiles.effective(profile,
                Map.of(), Map.of(), List.of()));
        NodeEmitter emitter = new NodeEmitter(new DefaultNodeRenderer());
        EmittedArtifacts artifacts = emitter.emit(model, profile);
        return artifacts.utf8TextByPath();
    }
}
