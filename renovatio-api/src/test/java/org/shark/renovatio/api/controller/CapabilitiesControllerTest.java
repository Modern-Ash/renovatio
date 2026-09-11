package org.shark.renovatio.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CapabilitiesControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void exposesSharedCapabilitiesContract() throws Exception {
        mvc.perform(get("/api/v1/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("renovatio.surface-capabilities"))
                .andExpect(jsonPath("$.version").value("2026-09-11.ac13"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'cobol.analyze')].maturity").value("stable"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'python.lab')].maturity").value("unsupported"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'python.lab')].surfaces.api").value("unsupported"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'node.target')].maturity").value("experimental"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'node.target')].surfaces.api").value("experimental"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'node.target')].surfaces.cli").value("experimental"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'reference-pipeline')].authorization")
                        .value("project-member"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'reference-pipeline')].states[0]")
                        .value("PENDING"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'reference-pipeline')].errors[0]")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'reference-pipeline')].manifestHashes[0]")
                        .value("sourceTreeHash"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'reference-pipeline')].runtime.equivalence")
                        .value("supported"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'java.plan')].surfaces.cli")
                        .value("planned"))
                .andExpect(jsonPath("$.capabilities[?(@.id == 'cobol.plan')].surfaces.cli")
                        .value("supported"));
    }
}
