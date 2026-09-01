package org.shark.renovatio.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectDecisionRepository;
import org.shark.renovatio.api.repository.ProjectProfileRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.api.service.DecisionLayerService;
import org.shark.renovatio.profile.MigrationProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArchitecturePreviewApiTest {
    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. PREVIEW.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(20).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    @Autowired MockMvc mvc;
    @Autowired ProjectRepository projects;
    @Autowired ProjectProfileRepository profiles;
    @Autowired ProjectDecisionRepository decisions;
    @Autowired DecisionLayerService decisionLayer;
    @TempDir Path workspace;

    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        decisions.deleteAll();
        profiles.deleteAll();
        projects.deleteAll();
        Files.writeString(workspace.resolve("preview.cob"), COBOL);
        projectId = projects.save(ProjectEntity.builder()
                .name("Architecture preview")
                .workspacePath(workspace.toString())
                .branch("main")
                .build()).getId();
    }

    @Test
    void exposesCanonicalReadOnlyPreview() throws Exception {
        mvc.perform(get("/api/projects/{projectId}/architecture-preview", projectId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("1"))
                .andExpect(jsonPath("$.requestHash").isString())
                .andExpect(jsonPath("$.profileHash").isString())
                .andExpect(jsonPath("$.programs[0].programId").value("PREVIEW"))
                .andExpect(jsonPath("$.programs[0].fallback").value(false))
                .andExpect(jsonPath("$.modules.length()").value(1))
                .andExpect(jsonPath("$.components.length()").value(3))
                .andExpect(jsonPath("$.artifacts[0].path").value("PreviewDTO.java"))
                .andExpect(jsonPath("$.artifacts[2].path").value("PreviewServiceImpl.java"))
                .andExpect(jsonPath("$.hasFallback").value(false));

        assertThat(workspace.resolve("generated-java-stubs")).doesNotExist();
        assertThat(workspace.resolve("manual-action-items.json")).doesNotExist();
    }

    @Test
    void enforcesProjectAndRoleBoundariesWithStructuredErrors() throws Exception {
        mvc.perform(get("/api/projects/{projectId}/architecture-preview", projectId)
                        .header("X-Role", "VIEWER"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/missing/architecture-preview")
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void reportsInactiveStylesWithoutWritingArtifacts() throws Exception {
        MigrationProfile overlay = new MigrationProfile("1", java.util.Map.of(), null,
                new MigrationProfile.Architecture(MigrationProfile.ArchitectureStyle.LAYERED_MVC,
                        MigrationProfile.ModuleGrouping.BY_PROGRAM), null, null, null, null);
        decisionLayer.replaceProfile(projectId, overlay, 0);

        mvc.perform(get("/api/projects/{projectId}/architecture-preview", projectId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ARCHITECTURE_STYLE_NOT_ACTIVE"))
                .andExpect(jsonPath("$.activeStyles[0]").value("HEXAGONAL"))
                .andExpect(jsonPath("$.activeStyles[1]").value("TRANSACTION_SCRIPT"));

        assertThat(workspace.resolve("generated-java-stubs")).doesNotExist();
    }

    @Test
    void reportsAnEmptyWorkspaceWithAMachineCode() throws Exception {
        Files.delete(workspace.resolve("preview.cob"));

        mvc.perform(get("/api/projects/{projectId}/architecture-preview", projectId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COBOL_SOURCE_NOT_FOUND"));
    }

    @Test
    void previewsDraftArchitectureWithoutPersistingIt() throws Exception {
        mvc.perform(get("/api/projects/{projectId}/architecture-preview", projectId)
                        .queryParam("style", "HEXAGONAL")
                        .queryParam("moduleGrouping", "BY_PROGRAM")
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programs[0].requestedStyle").value("HEXAGONAL"))
                .andExpect(jsonPath("$.artifacts[0].path")
                        .value("modules/preview/application/port/in/PreviewService.java"));

        assertThat(decisionLayer.profile(projectId).profile().architecture()).isNull();
        assertThat(workspace.resolve("generated-java-stubs")).doesNotExist();
    }
}
