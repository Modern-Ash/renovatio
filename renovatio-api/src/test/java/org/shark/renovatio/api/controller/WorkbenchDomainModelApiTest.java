package org.shark.renovatio.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.DomainSuggestionDecisionRepository;
import org.shark.renovatio.api.repository.ProjectDomainModelVersionRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.domain.model.DomainModel.DomainNode;
import org.shark.renovatio.domain.model.DomainModel.Evidence;
import org.shark.renovatio.domain.model.DomainModel.Kind;
import org.shark.renovatio.domain.model.DomainModel.Origin;
import org.shark.renovatio.domain.model.DomainModel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkbenchDomainModelApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProjectRepository projects;
    @Autowired ProjectDomainModelVersionRepository versions;
    @Autowired DomainSuggestionDecisionRepository decisions;
    private String projectId;

    @BeforeEach
    void setUp() {
        decisions.deleteAll();
        versions.deleteAll();
        projects.deleteAll();
        projectId = projects.save(ProjectEntity.builder().name("Domain API")
                .workspacePath("/tmp/domain-api").branch("main").build()).getId();
    }

    @Test
    void readsEmptyModelAndRequiresModifyRoleForVersionedSave() throws Exception {
        String path = "/api/projects/" + projectId + "/workbench/domain-model";
        mvc.perform(get(path).header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(0))
                .andExpect(jsonPath("$.model.nodes").isEmpty());
        mvc.perform(get(path).header("X-Role", "VIEWER")).andExpect(status().isForbidden());

        DomainModel model = new DomainModel("1", projectId, List.of(), List.of(), List.of());
        String request = json.writeValueAsString(Map.of("expectedRevision", 0, "model", model));
        mvc.perform(put(path).header("X-Role", "VIEWER").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        mvc.perform(put(path).header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
        mvc.perform(put(path).header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("expectedRevision", 0, "model", model))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVISION_CONFLICT"));
    }

    @Test
    void editsReloadsComparesRestoresAndRecordsSuggestionDecision() throws Exception {
        String path = "/api/projects/" + projectId + "/workbench/domain-model";
        Evidence evidence = new Evidence("src/PAYROLL.CBL#CUSTOMER", "sha256:" + "a".repeat(64), "COBOL record");
        DomainNode suggested = new DomainNode("customer", Kind.ENTITY, "Suggested customer",
                List.of(new Property("number", "PIC-9", true, List.of(evidence))),
                List.of(evidence), Origin.LLM, 0.82);
        DomainModel first = new DomainModel("1", projectId, List.of(suggested), List.of(), List.of());
        mvc.perform(put(path).header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("expectedRevision", 0, "model", first))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));

        DomainNode corrected = new DomainNode("customer", Kind.AGGREGATE, "Customer account",
                suggested.properties(), suggested.evidence(), Origin.LLM, 0.91);
        DomainModel second = new DomainModel("1", projectId, List.of(corrected), List.of(), List.of());
        mvc.perform(put(path).header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("expectedRevision", 1, "model", second))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(2));
        mvc.perform(get(path).header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model.nodes[0].name").value("Customer account"))
                .andExpect(jsonPath("$.model.nodes[0].properties[0].name").value("number"));
        mvc.perform(get(path + "/versions").header("X-Role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].revision").value(2));
        mvc.perform(get(path + "/compare").param("from", "1").param("to", "2").header("X-Role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.changed[0].targetId").value("customer"));

        mvc.perform(post(path + "/versions/1:restore").header("X-Role", "MANAGER")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedRevision\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(3))
                .andExpect(jsonPath("$.model.nodes[0].name").value("Suggested customer"));
        mvc.perform(post(path + "/suggestions/node:customer").header("X-Role", "MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"accepted\",\"expectedRevision\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].status").value("accepted"));
    }

    @Test
    void rejectsRelationThatReferencesUnknownNode() throws Exception {
        String path = "/api/projects/" + projectId + "/workbench/domain-model";
        String request = json.writeValueAsString(Map.of(
                "expectedRevision", 0,
                "model", Map.of(
                        "schemaVersion", "1", "projectId", projectId,
                        "nodes", List.of(),
                        "relations", List.of(Map.of(
                                "id", "broken", "fromId", "missing-a", "toId", "missing-b",
                                "kind", "USES", "sourceCardinality", "ONE", "targetCardinality", "ONE")),
                        "invariants", List.of())));
        mvc.perform(put(path).header("X-Role", "MANAGER").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path).header("X-Role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(0));
    }
}
