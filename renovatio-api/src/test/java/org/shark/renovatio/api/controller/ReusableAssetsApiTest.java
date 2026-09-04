package org.shark.renovatio.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectDecisionRepository;
import org.shark.renovatio.api.repository.ProjectProfileRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.api.service.DecisionLayerService;
import org.shark.renovatio.llm.decision.ArchitectureSuggestionGateway;
import org.shark.renovatio.llm.decision.DecisionSuggestionService;
import org.shark.renovatio.profile.MigrationProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReusableAssetsApiTest {
    @Autowired MockMvc mvc;
    @Autowired ProjectRepository projects;
    @Autowired ProjectProfileRepository profiles;
    @Autowired ProjectDecisionRepository decisions;
    @Autowired DecisionLayerService decisionLayer;
    @MockBean ArchitectureSuggestionGateway architectureSuggestions;

    private String source;
    private String target;
    private String assetName;

    @BeforeEach
    void setUp() {
        when(architectureSuggestions.suggest(anyList(), anyString(), any(), any(Instant.class)))
                .thenAnswer(invocation -> new DecisionSuggestionService.SuggestionBatch(
                        List.copyOf(invocation.getArgument(0)), 0, 0, 0));
        decisions.deleteAll();
        profiles.deleteAll();
        projects.deleteAll();
        source = project("A");
        target = project("B");
        assetName = "bank-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void reusesAnExplicitTemplateAndPolicyVersionAcrossProjects() throws Exception {
        var overlay = new MigrationProfile("1", Map.of("dialect", "IBM"),
                new MigrationProfile.Target(MigrationProfile.Language.JAVA, "21"), null, null, null, null, null);
        decisionLayer.replaceProfile(source, overlay, 0);
        decisionLayer.upsertAnalysis(source, "a".repeat(64));
        decisionLayer.bulkConfirm(source, java.math.BigDecimal.ONE);

        mvc.perform(post("/api/profile-templates").header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + assetName + "\",\"version\":\"1\",\"projectId\":\"" + source + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value("1"));
        mvc.perform(post("/api/policy-catalogs").header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + assetName + "\",\"version\":\"1\",\"projectId\":\"" + source + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.entries.length()").value(7));

        decisionLayer.upsertAnalysis(target, "b".repeat(64));
        String reference = "{\"name\":\"" + assetName + "\",\"version\":\"1\"}";
        mvc.perform(post("/api/projects/{id}/profile-template", target).header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(reference))
                .andExpect(status().isOk());
        mvc.perform(post("/api/projects/{id}/policy-catalog", target).header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(reference))
                .andExpect(status().isOk()).andExpect(jsonPath("$.autoConfirmed").value(7));

        mvc.perform(get("/api/projects/{id}/profile:effective", target).header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.target.languageVersion").value("21"));
        mvc.perform(get("/api/projects/{id}/decisions", target).header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].source").value("POLICY"))
                .andExpect(jsonPath("$.items[0].policyProvenance.catalogVersion").value("1"));
        mvc.perform(get("/api/profile-templates").header("X-Role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].projects[0]").value(target));
    }

    @Test
    void rejectsTraversalAndMissingVersionsWithStableStatuses() throws Exception {
        mvc.perform(post("/api/profile-templates").header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"../escape\",\"version\":\"1\",\"projectId\":\"" + source + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(post("/api/projects/{id}/profile-template", target).header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"missing\",\"version\":\"1\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private String project(String name) {
        return projects.save(ProjectEntity.builder().name(name).workspacePath("/tmp/" + name).branch("main").build()).getId();
    }
}
