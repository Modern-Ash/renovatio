package org.shark.renovatio.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectArchitectureProfileVersionRepository;
import org.shark.renovatio.api.repository.ProjectDecisionRepository;
import org.shark.renovatio.api.repository.ProjectProfileRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.api.service.DecisionLayerService;
import org.shark.renovatio.profile.MigrationProfile;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @Autowired ProjectArchitectureProfileVersionRepository architectureProfiles;
    @Autowired ProjectDecisionRepository decisions;
    @Autowired DecisionLayerService decisionLayer;
    @TempDir Path workspace;

    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        decisions.deleteAll();
        architectureProfiles.deleteAll();
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
    void previewsLayeredMvcWithoutWritingArtifacts() throws Exception {
        MigrationProfile overlay = new MigrationProfile("1", java.util.Map.of(), null,
                new MigrationProfile.Architecture(MigrationProfile.ArchitectureStyle.LAYERED_MVC,
                        MigrationProfile.ModuleGrouping.BY_PROGRAM), null, null, null, null);
        decisionLayer.replaceProfile(projectId, overlay, 0);

        mvc.perform(get("/api/projects/{projectId}/architecture-preview", projectId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programs[0].requestedStyle").value("LAYERED_MVC"))
                .andExpect(jsonPath("$.components.length()").value(4));

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

    @Test
    void exposesVersionedWorkbenchArchitectureCanvas() throws Exception {
        String path = "/api/projects/{projectId}/workbench/architecture/canvas";
        String request = """
                {
                  "expectedRevision": 0,
                  "profile": {
                    "style": "LAYERED_MVC",
                    "moduleGrouping": "BY_PROGRAM",
                    "framework": "SPRING_BOOT",
                    "persistence": "JPA",
                    "packageRoots": {
                      "service": "com.acme.services",
                      "model": "com.acme.domain"
                    },
                    "suffixes": {
                      "service": "UseCase",
                      "model": "Record"
                    },
                    "classNames": {},
                    "dependencyRules": [
                      { "fromLayer": "service", "toLayer": "model", "allowed": false, "reason": "service cannot read model directly" }
                    ]
                  }
                }
                """;

        mvc.perform(get(path, projectId).header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(0))
                .andExpect(jsonPath("$.profile.style").value("TRANSACTION_SCRIPT"));
        mvc.perform(put(path, projectId).header("X-Role", "VIEWER").contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
        mvc.perform(put(path, projectId).header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(1))
                .andExpect(jsonPath("$.canonicalHash").isString())
                .andExpect(jsonPath("$.canvas[?(@.layer == 'controller')]").isArray())
                .andExpect(jsonPath("$.manifest[?(@.path == 'com/acme/services/PreviewServiceUseCase.java')]").isArray())
                .andExpect(jsonPath("$.dependencyDiagnostics[0].code").value("FORBIDDEN_DEPENDENCY"));
        mvc.perform(get(path + "/versions", projectId).header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revision").value(1))
                .andExpect(jsonPath("$[0].style").value("LAYERED_MVC"));
        mvc.perform(get(path + "/compare", projectId).param("from", "1").param("to", "1")
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed.length()").value(0));
        mvc.perform(put(path, projectId).header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVISION_CONFLICT"));

        assertThat(workspace.resolve("generated-java-stubs")).doesNotExist();
    }
}
