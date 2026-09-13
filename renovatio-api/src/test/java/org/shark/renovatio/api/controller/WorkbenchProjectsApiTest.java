package org.shark.renovatio.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.JobRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

@SpringBootTest(properties = {
        "renovatio.workbench.dev-no-auth-enabled=true",
        "renovatio.workbench.allowed-origin=http://127.0.0.1:3000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkbenchProjectsApiTest {
    @Autowired MockMvc mvc;
    @Autowired ProjectRepository projects;
    @Autowired JobRepository jobs;
    @TempDir Path workspace;

    private String projectId;

    @BeforeEach
    void setUp() {
        jobs.deleteAll();
        projects.deleteAll();
        projectId = projects.save(ProjectEntity.builder()
                .name("Workbench project")
                .workspacePath(workspace.toString())
                .branch("main")
                .build()).getId();
    }

    @Test
    void exposesProjectDetailsAndAnalysisJobsThroughCorsEnabledWorkbenchRoutes() throws Exception {
        mvc.perform(get("/api/workbench/projects/{projectId}", projectId)
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:3000"))
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.workspacePath").value(workspace.toString()))
                .andExpect(jsonPath("$.cobolScanRoot").value(workspace.toString()));

        String jobId = mvc.perform(post("/api/workbench/projects/{projectId}/jobs", projectId)
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "operation": "analyze", "params": { "workspacePath": "%s" } }
                                """.formatted(workspace)))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:3000"))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/workbench/jobs/{jobId}", jobId)
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:3000"))
                .andExpect(jsonPath("$.id").value(jobId));
    }
}
