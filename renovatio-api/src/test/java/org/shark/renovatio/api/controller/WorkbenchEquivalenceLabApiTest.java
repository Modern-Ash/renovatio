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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkbenchEquivalenceLabApiTest {
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
        Files.writeString(workspace.resolve("generated-java-stubs/com/acme/PreviewService.java"), "class PreviewService {}\n");
        Files.writeString(workspace.resolve("equivalence-divergence.json"), """
                {"fixtureId":"divergence-case","name":"Divergence case","classification":"INFO","reason":"fixture","blocksRelease":false}
                """);
        projectId = projects.save(ProjectEntity.builder()
                .name("Equivalence lab")
                .workspacePath(workspace.toString())
                .branch("main")
                .build()).getId();
    }

    @Test
    void executesRepeatableCaseExportsReportAndRequiresTriageBeforePromotion() throws Exception {
        mvc.perform(get("/api/projects/{projectId}/workbench/equivalence", projectId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixtures[0].id").value("divergence-case"))
                .andExpect(jsonPath("$.gate.status").value("blocked"))
                .andExpect(jsonPath("$.gate.blockers[0]").value("no completed equivalence run"));

        JsonNode run = mapper.readTree(mvc.perform(post("/api/projects/{projectId}/workbench/equivalence/runs", projectId)
                        .header("X-Role", "MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fixtureId": "divergence-case",
                                  "baselineId": "cobol-baseline",
                                  "candidateId": "generated-java-stubs/com/acme/PreviewService.java",
                                  "inputOverrides": { "account": "1001" },
                                  "commit": "abc123",
                                  "profileHash": "sha256:profile",
                                  "changeSetId": "cs-123"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("completed"))
                .andExpect(jsonPath("$.progress").value(100))
                .andExpect(jsonPath("$.logs[0]").value("queued fixture divergence-case"))
                .andExpect(jsonPath("$.divergences[0].evidenceRef").value("equivalence-divergence.json"))
                .andExpect(jsonPath("$.changeSetId").value("cs-123"))
                .andReturn().getResponse().getContentAsString());
        String runId = run.get("id").asText();
        String divergenceId = run.get("divergences").get(0).get("id").asText();

        mvc.perform(get("/api/projects/{projectId}/workbench/equivalence", projectId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gate.status").value("blocked"))
                .andExpect(jsonPath("$.history[0].action").value("run-completed"));
        JsonNode triagedRun = mapper.readTree(mvc.perform(post("/api/projects/{projectId}/workbench/equivalence/runs/{runId}/divergences/{divergenceId}:triage",
                        projectId, runId, divergenceId)
                        .header("X-Role", "MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "action": "accepted-difference", "reason": "approved fixture delta" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.divergences[0].triageStatus").value("accepted-difference"))
                .andReturn().getResponse().getContentAsString());
        String triagedReportHash = triagedRun.get("reportHash").asText();
        mvc.perform(get("/api/projects/{projectId}/workbench/equivalence", projectId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gate.status").value("ready"))
                .andExpect(jsonPath("$.gate.promotable").value(true));
        mvc.perform(post("/api/projects/{projectId}/workbench/equivalence/runs/{runId}:repeat", projectId, runId)
                .header("X-Role", "MANAGER"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reportHash").value(triagedReportHash))
                .andExpect(jsonPath("$.logs[?(@ =~ /.*repeated exactly.*/)]").isArray());
        mvc.perform(get("/api/projects/{projectId}/workbench/equivalence/runs/{runId}/report", projectId, runId)
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("1"))
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.reportHash").isString())
                .andExpect(jsonPath("$.commit").value("abc123"));
    }

    @Test
    void forbidsViewerRunAndRecordsCancellation() throws Exception {
        mvc.perform(post("/api/projects/{projectId}/workbench/equivalence/runs", projectId)
                        .header("X-Role", "VIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fixtureId\":\"divergence-case\"}"))
                .andExpect(status().isForbidden());
        JsonNode run = mapper.readTree(mvc.perform(post("/api/projects/{projectId}/workbench/equivalence/runs", projectId)
                        .header("X-Role", "MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fixtureId\":\"divergence-case\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/projects/{projectId}/workbench/equivalence/runs/{runId}:cancel", projectId, run.get("id").asText())
                        .header("X-Role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("cancelled"))
                .andExpect(jsonPath("$.logs[?(@ =~ /.*cancelled.*/)]").isArray());
    }
}
