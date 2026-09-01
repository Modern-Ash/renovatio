package org.shark.renovatio.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectDecisionRepository;
import org.shark.renovatio.api.repository.ProjectProfileRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.api.service.DecisionLayerService;
import org.shark.renovatio.api.service.ProjectService;
import org.shark.renovatio.decisions.DecisionStore;
import org.shark.renovatio.decisions.DecisionTransitions;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DecisionLayerApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProjectRepository projects;
    @Autowired ProjectProfileRepository profiles;
    @Autowired ProjectDecisionRepository decisions;
    @Autowired DecisionLayerService service;
    @SpyBean DecisionStore decisionStore;
    @Autowired ProjectService projectService;

    private String projectId;

    @BeforeEach
    void setUp() {
        decisions.deleteAll();
        profiles.deleteAll();
        projects.deleteAll();
        projectId = projects.save(ProjectEntity.builder()
                .name("Decision test")
                .workspacePath("/tmp/decision-test")
                .branch("main")
                .build()).getId();
    }

    @Test
    void profileUsesQuotedEtagAndIdenticalPutPreservesRevision() throws Exception {
        mvc.perform(get(path("/profile")).header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.schemaVersion").value("1"))
                .andExpect(jsonPath("$.extensions").isMap());

        String overlay = """
                {"schemaVersion":"1","extensions":{},"target":{"language":"JAVA","languageVersion":"21"}}
                """;
        mvc.perform(put(path("/profile")).header("X-Role", "MANAGER")
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content(overlay))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"1\""));
        mvc.perform(put(path("/profile")).header("X-Role", "MANAGER")
                        .header("If-Match", "\"1\"").contentType(MediaType.APPLICATION_JSON).content(overlay))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"1\""));
        mvc.perform(put(path("/profile")).header("X-Role", "MANAGER")
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content(overlay))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVISION_CONFLICT"));
    }

    @Test
    void profileRejectsInvalidCrossFieldConfigurationAndUnknownFields() throws Exception {
        String invalid = """
                {"schemaVersion":"1","extensions":{},"llm":{"enabled":false,"suggestDecisions":true,"maxSuggestionsPerRun":0}}
                """;
        mvc.perform(put(path("/profile")).header("X-Role", "ADMIN")
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PROFILE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations.length()").value(2));
        mvc.perform(put(path("/profile")).header("X-Role", "ADMIN")
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaVersion\":\"1\",\"extensions\":{},\"unexpected\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PROFILE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].path").value("/unexpected"));
        mvc.perform(put(path("/profile")).header("X-Role", "ADMIN")
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaVersion\":\"1\",\"extensions\":{},\"target\":{\"language\":\"RUBY\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].path").value("/target/language"));
    }

    @Test
    void profilePersistsAndReturnsNullExtensionValues() throws Exception {
        String overlay = """
                {"schemaVersion":"1","extensions":{"vendorSetting":null}}
                """;

        mvc.perform(put(path("/profile")).header("X-Role", "MANAGER")
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content(overlay))
                .andExpect(status().isOk());
        String response = mvc.perform(get(path("/profile")).header("X-Role", "ADMIN"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(response).path("extensions").has("vendorSetting")).isTrue();
        assertThat(json.readTree(response).path("extensions").path("vendorSetting").isNull()).isTrue();
    }

    @Test
    void analysisCreatesSevenDeterministicDecisionsAndSupportsTransitions() throws Exception {
        service.upsertAnalysis(projectId, "a".repeat(64));
        String response = mvc.perform(get(path("/decisions")).header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.items[0].confidence").value(1.0))
                .andReturn().getResponse().getContentAsString();
        JsonNode first = json.readTree(response).path("items").get(0);

        mvc.perform(patch(path("/decisions/" + first.path("id").asText()))
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PatchRequest(first.path("chosenOption").asText(), 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.revision").value(2));

        mvc.perform(post(path("/decisions:bulk-confirm")).header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minConfidence\":1.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(6))
                .andExpect(jsonPath("$.skipped").value(1));
        mvc.perform(get(path("/decisions?status=CONFIRMED&minConfidence=1"))
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(7));
    }

    @Test
    void concurrentPatchesWithTheSameRevisionReturnOneConflict() throws Exception {
        service.upsertAnalysis(projectId, "9".repeat(64));
        var decision = decisionStore.findAll(projectId).get(0);
        CyclicBarrier bothLoaded = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            bothLoaded.await(5, TimeUnit.SECONDS);
            return result;
        }).when(decisionStore).findById(projectId, decision.id());

        String content = json.writeValueAsString(new PatchRequest(decision.chosenOption(), decision.revision()));
        Callable<Integer> request = () -> mvc.perform(patch(path("/decisions/" + decision.id()))
                        .header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON).content(content))
                .andReturn().getResponse().getStatus();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void projectScopeAndRoleChecksDoNotLeakDecisions() throws Exception {
        service.upsertAnalysis(projectId, "b".repeat(64));
        String other = projects.save(ProjectEntity.builder().name("Other")
                .workspacePath("/tmp/other").build()).getId();
        mvc.perform(get("/api/projects/{id}/decisions", other).header("X-Role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(path("/decisions")).header("X-Role", "VIEWER"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/missing/decisions").header("X-Role", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reanalysisRestoresAPreviouslyRetiredStableDecision() {
        service.upsertAnalysis(projectId, "d".repeat(64));
        var original = decisionStore.findAll(projectId).get(0);
        decisionStore.save(projectId, DecisionTransitions.retire(original, java.time.Instant.now()));
        assertThat(decisionStore.find(projectId, null, null, null)).hasSize(6);

        service.upsertAnalysis(projectId, "e".repeat(64));

        var restored = decisionStore.findById(projectId, original.id()).orElseThrow();
        assertThat(restored.active()).isTrue();
        assertThat(restored.semanticIrHash()).isEqualTo("e".repeat(64));
    }

    @Test
    void projectDeletionCascadesProfileAndDecisionState() {
        service.replaceProfile(projectId, MigrationProfiles.emptyOverlay(), 0);
        service.upsertAnalysis(projectId, "f".repeat(64));

        projectService.deleteProject(projectId);

        assertThat(projects.existsById(projectId)).isFalse();
        assertThat(profiles.existsById(projectId)).isFalse();
        assertThat(decisionStore.findAll(projectId)).isEmpty();
    }

    @Test
    void effectiveProfileIsDeterministicAndRejectsMalformedFilters() throws Exception {
        service.upsertAnalysis(projectId, "c".repeat(64));
        mvc.perform(post(path("/decisions:bulk-confirm")).header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minConfidence\":1.0}"))
                .andExpect(status().isOk());
        String one = mvc.perform(get(path("/profile:effective")).header("X-Role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileHash").isString())
                .andExpect(jsonPath("$.appliedDecisionIds.length()").value(7))
                .andReturn().getResponse().getContentAsString();
        String two = mvc.perform(get(path("/profile:effective")).header("X-Role", "ADMIN"))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(one).path("profileHash")).isEqualTo(json.readTree(two).path("profileHash"));
        mvc.perform(get(path("/decisions?minConfidence=1.1")).header("X-Role", "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    private String path(String suffix) { return "/api/projects/" + projectId + suffix; }
    private record PatchRequest(String chosenOption, long revision) { }
}
