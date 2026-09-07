package org.shark.renovatio.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectArchitectureProfileVersionRepository;
import org.shark.renovatio.api.repository.ProjectDecisionRepository;
import org.shark.renovatio.api.repository.ProjectProfileRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "renovatio.workbench.dev-write-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkbenchChangeSetApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProjectRepository projects;
    @Autowired ProjectProfileRepository profiles;
    @Autowired ProjectArchitectureProfileVersionRepository architectureProfiles;
    @Autowired ProjectDecisionRepository decisions;
    @TempDir Path workspace;

    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        decisions.deleteAll();
        architectureProfiles.deleteAll();
        profiles.deleteAll();
        projects.deleteAll();
        Files.createDirectories(workspace.resolve("generated-java-stubs/com/acme"));
        Files.writeString(workspace.resolve("generated-java-stubs/com/acme/PreviewService.java"), "class PreviewService { int v = 1; }\n");
        projectId = projects.save(ProjectEntity.builder()
                .name("Change set")
                .workspacePath(workspace.toString())
                .branch("main")
                .build()).getId();
    }

    @Test
    void requiresDiffApprovalAndDoubleConfirmationBeforeApplyThenRollsBack() throws Exception {
        String id = createChangeSet("class PreviewService { int v = 2; }\n", true);

        mvc.perform(post("/api/projects/{projectId}/workbench/change-sets/{id}:approve", projectId, id)
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "diffReviewed": true, "confirmationPhrase": "APPROVE DANGEROUS CHANGE SET", "reason": "ready" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHANGE_SET_REJECTED"));

        mvc.perform(get("/api/projects/{projectId}/workbench/change-sets/{id}/diff", projectId, id)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diff.requiredBeforeApproval[0]").value("diff-viewed"))
                .andExpect(jsonPath("$.files[0].path").value("generated-java-stubs/com/acme/PreviewService.java"));
        mvc.perform(post("/api/projects/{projectId}/workbench/change-sets/{id}:submit-review", projectId, id)
                        .header("X-Role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("review"));
        mvc.perform(post("/api/projects/{projectId}/workbench/change-sets/{id}:approve", projectId, id)
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "diffReviewed": true, "confirmationPhrase": "APPROVE DANGEROUS CHANGE SET", "reason": "approved by reviewer" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("approved"))
                .andExpect(jsonPath("$.approvedManifestHash").isString());
        mvc.perform(post("/api/projects/{projectId}/workbench/change-sets/{id}:apply", projectId, id)
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "confirmationPhrase": "APPLY APPROVED CHANGE SET", "reason": "apply approved manifest" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("applied"))
                .andExpect(jsonPath("$.history[?(@.action == 'applied')]").isArray());

        assertThat(Files.readString(workspace.resolve("generated-java-stubs/com/acme/PreviewService.java"))).contains("v = 2");

        mvc.perform(post("/api/projects/{projectId}/workbench/change-sets/{id}:rollback", projectId, id)
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "confirmationPhrase": "ROLL BACK APPLIED CHANGE SET", "reason": "verification rollback" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("rolled-back"))
                .andExpect(jsonPath("$.history[?(@.action == 'rolled-back')]").isArray());

        assertThat(Files.readString(workspace.resolve("generated-java-stubs/com/acme/PreviewService.java"))).contains("v = 1");
    }

    @Test
    void rejectsViewerMutationAndRejectedChangeSetDoesNotAlterWorkspace() throws Exception {
        mvc.perform(post("/api/projects/{projectId}/workbench/change-sets", projectId)
                        .header("X-Role", "VIEWER").contentType(MediaType.APPLICATION_JSON)
                        .content(changeSetRequest("class PreviewService { int v = 3; }\n", false)))
                .andExpect(status().isForbidden());

        String id = createChangeSet("class PreviewService { int v = 4; }\n", false);
        mvc.perform(post("/api/projects/{projectId}/workbench/change-sets/{id}:reject", projectId, id)
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "not wanted" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("rejected"));
        mvc.perform(post("/api/projects/{projectId}/workbench/change-sets/{id}:apply", projectId, id)
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "confirmationPhrase": "APPLY APPROVED CHANGE SET", "reason": "should fail" }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(Files.readString(workspace.resolve("generated-java-stubs/com/acme/PreviewService.java"))).contains("v = 1");
    }

    @Test
    void listsProjectHistoryWithManifestEvidenceAndHashes() throws Exception {
        String id = createChangeSet("class PreviewService { int v = 5; }\n", false);

        mvc.perform(get("/api/projects/{projectId}/workbench/change-sets", projectId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].manifestHash").isString())
                .andExpect(jsonPath("$[0].decisions[0]").value("decision:architecture-layer"))
                .andExpect(jsonPath("$[0].evidence[0]").value("shadow-impact:manifest"));
    }

    private String createChangeSet(String proposedContent, boolean dangerous) throws Exception {
        String response = mvc.perform(post("/api/projects/{projectId}/workbench/change-sets", projectId)
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content(changeSetRequest(proposedContent, dangerous)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("draft"))
                .andExpect(jsonPath("$.manifestHash").isString())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(response);
        return json.get("id").asText();
    }

    private String changeSetRequest(String proposedContent, boolean dangerous) {
        return """
                {
                  "title": "Apply architecture proposal",
                  "dangerous": %s,
                  "files": [
                    { "path": "generated-java-stubs/com/acme/PreviewService.java", "action": "modify", "proposedContent": %s }
                  ],
                  "decisions": ["decision:architecture-layer"],
                  "evidence": ["shadow-impact:manifest"]
                }
                """.formatted(dangerous, mapper.valueToTree(proposedContent).toString());
    }
}
