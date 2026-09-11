package org.shark.renovatio.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.api.entity.ProjectEntity;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReferencePipelineApiTest {
    @Autowired MockMvc mvc;
    @Autowired ProjectRepository projects;

    private String projectId;
    private Path fixture;
    private Path output;

    @BeforeEach
    void setUp() throws Exception {
        projects.deleteAll();
        fixture = Path.of("..", "renovatio-provider-cobol", "src", "test", "resources", "fixtures",
                "batch-simple").toAbsolutePath().normalize();
        output = Files.createTempDirectory("renovatio-reference-api-");
        projectId = projects.save(ProjectEntity.builder()
                .name("Reference pipeline")
                .workspacePath(fixture.toString())
                .javaOutputPath(output.toString())
                .branch("main")
                .build()).getId();
    }

    @Test
    void exposesReferencePipelineThroughApi() throws Exception {
        mvc.perform(post("/api/projects/{projectId}/reference-pipeline", projectId)
                        .header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "verifyEquivalence": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.fixtureId").value("batch-simple"))
                .andExpect(jsonPath("$.build.success").value(true))
                .andExpect(jsonPath("$.equivalenceReports[0].decision").value("PASS"));

        assertThat(output.resolve("src/main/java/org/shark/renovatio/generated/cobol/Batch001Service.java"))
                .isRegularFile();
    }
}
