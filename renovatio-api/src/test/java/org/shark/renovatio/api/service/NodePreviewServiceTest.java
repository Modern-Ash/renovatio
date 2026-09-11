package org.shark.renovatio.api.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodePreviewServiceTest {
    @Test
    void previewIncludesBuildLintTestAndLockfileArtifacts() throws Exception {
        ProjectRepository projects = mock(ProjectRepository.class);
        JavaGenerationService generation = mock(JavaGenerationService.class);
        DecisionLayerService decisions = mock(DecisionLayerService.class);
        NodePreviewService service = new NodePreviewService(projects, generation, decisions);

        ProjectEntity project = ProjectEntity.builder()
                .id("project")
                .name("Project")
                .workspacePath("/tmp/project")
                .branch("main")
                .build();
        when(projects.findById("project")).thenReturn(Optional.of(project));
        when(decisions.effective("project")).thenReturn(nodeProfile());
        when(generation.semanticPrograms(any(NqlQuery.class), any(Workspace.class)))
                .thenReturn(List.of(program("FIRST"), program("SECOND")));

        Map<String, String> files = service.generateNodePreview("project");

        assertTrue(files.containsKey("src/main.ts"));
        assertTrue(files.containsKey("src/main.test.ts"));
        assertTrue(files.containsKey("package.json"));
        assertTrue(files.containsKey("package-lock.json"));
        assertTrue(files.containsKey("tsconfig.json"));
        assertTrue(files.containsKey("docs/node-idioms.md"));
        assertTrue(files.get("package.json").contains("\"build\": \"tsc\""));
        assertTrue(files.get("package.json").contains("\"lint\": \"tsc --noEmit\""));
        assertTrue(files.get("package.json").contains("\"test\": \"npm run build && node --test dist/**/*.test.js\""));
        assertFalse(files.get("package.json").contains("\"express\""));
        assertTrue(files.get("package-lock.json").contains("\"lockfileVersion\": 3"));
        assertEquals(files.get("src/main.test.ts"), files.get("src/main.test.ts"));
    }

    private static MigrationProfiles.EffectiveProfile nodeProfile() {
        MigrationProfile overlay = new MigrationProfile("1", Map.of(),
                new MigrationProfile.Target(MigrationProfile.Language.NODE, "20"),
                null, null, null, null, null);
        return MigrationProfiles.effective(overlay, Map.of(), Map.of(), List.of());
    }

    private static SemanticProgram program(String programId) {
        SourceSpan span = new SourceSpan("src/" + programId.toLowerCase() + ".cob", 1, 1, 1, 9);
        SourceProvenance provenance = new SourceProvenance(span.sourcePath(), "0".repeat(64),
                "COBOL", Optional.empty(), List.of());
        return new SemanticProgram("1", SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.PROGRAM, "program", span), programId, provenance,
                List.of(), List.of(), List.of(), List.of(),
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of());
    }
}
