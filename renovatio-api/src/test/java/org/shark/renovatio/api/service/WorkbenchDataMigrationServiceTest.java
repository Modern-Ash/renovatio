package org.shark.renovatio.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.DomainSuggestionDecisionRepository;
import org.shark.renovatio.api.repository.ProjectDomainModelVersionRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.domain.model.DomainModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WorkbenchDataMigrationServiceTest {
    @Autowired WorkbenchDataMigrationService service;
    @Autowired WorkbenchDomainModelService domainModels;
    @Autowired ProjectRepository projects;
    @Autowired ProjectDomainModelVersionRepository versions;
    @Autowired DomainSuggestionDecisionRepository decisions;

    private String projectId;

    @BeforeEach
    void setUp() {
        decisions.deleteAll();
        versions.deleteAll();
        projects.deleteAll();
        projectId = projects.save(ProjectEntity.builder().name("Data migration")
                .workspacePath("/tmp/data-migration").branch("main").build()).getId();
    }

    @Test
    void plansDryRunFromDomainPhysicalMappings() {
        domainModels.save(projectId, 0, mappedModel());

        var plan = service.plan(projectId);

        assertThat(plan.status()).isEqualTo("planned");
        assertThat(plan.tables()).hasSize(1);
        assertThat(plan.tables().get(0).targetTable()).isEqualTo("customers");
        assertThat(plan.tables().get(0).sourceDataset()).isEqualTo("AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS");
        assertThat(plan.tables().get(0).columns()).extracting(value -> value.targetColumn())
                .containsExactly("CUSTOMER_ID", "CUSTOMER_NAME");
        assertThat(plan.dryRun().status()).isEqualTo("dry-run-ok");
        assertThat(plan.dryRun().previewRows()).hasSize(1);
    }

    @Test
    void generatesGovernedChangeSetRequestWithRollbackEvidence() {
        domainModels.save(projectId, 0, mappedModel());

        var request = service.generateChangeSetRequest(projectId);

        assertThat(request.dangerous()).isTrue();
        assertThat(request.decisions()).contains("rollback-requires-snapshot-or-staging-swap");
        assertThat(request.files()).extracting(value -> value.path())
                .contains("generated-data-migration/" + projectId + "/README.md",
                        "generated-data-migration/" + projectId + "/staging-load.sql",
                        "generated-data-migration/" + projectId + "/dry-run-report.json");
        assertThat(request.files().stream().filter(file -> file.path().endsWith("staging-load.sql"))
                .findFirst().orElseThrow().proposedContent()).contains("CREATE TABLE IF NOT EXISTS staging_customers");
    }

    private DomainModel mappedModel() {
        var evidence = new DomainModel.Evidence("cpy/CUSTOMER.cpy:1", "COBOL", "customer layout");
        var customer = new DomainModel.DomainNode("customer", DomainModel.Kind.ENTITY, "Customer",
                List.of(new DomainModel.Property("id", "string", true, List.of(evidence), true,
                                "CUSTOMER_ID", "CUST-ID", "AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS"),
                        new DomainModel.Property("name", "string", true, List.of(evidence), false,
                                "CUSTOMER_NAME", "CUST-NAME", "AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS")),
                List.of(evidence), DomainModel.Origin.HUMAN, 1.0,
                "customers", "AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS");
        return new DomainModel("1", projectId, List.of(customer), List.of(), List.of());
    }
}
