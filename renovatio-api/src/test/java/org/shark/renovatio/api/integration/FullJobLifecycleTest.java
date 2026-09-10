package org.shark.renovatio.api.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.MigrationPlanSnapshotRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.api.repository.RunSnapshotRepository;
import org.shark.renovatio.api.service.PersistentPlanService;
import org.shark.renovatio.shared.domain.Scope;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FullJobLifecycleTest {

    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. ROUTED.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(20).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'API' TO CUSTOMER-NAME.
                DISPLAY CUSTOMER-NAME.
                STOP RUN.
            """;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MigrationPlanSnapshotRepository planRepository;
    @Autowired
    private RunSnapshotRepository runRepository;
    @Autowired
    private PersistentPlanService planService;
    @TempDir
    Path workspacePath;

    @BeforeEach
    void cleanPersistence() {
        runRepository.deleteAll();
        planRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveProject() {
        ProjectEntity entity = ProjectEntity.builder()
                .name("Test Project")
                .workspacePath("/path/to/workspace")
                .branch("main")
                .build();

        ProjectEntity saved = projectRepository.save(entity);
        assertNotNull(saved.getId());

        var retrieved = projectRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Test Project", retrieved.get().getName());
    }

    @Test
    void shouldListProjects() {
        projectRepository.save(ProjectEntity.builder()
                .name("Project 1")
                .workspacePath("/path/1")
                .build());

        projectRepository.save(ProjectEntity.builder()
                .name("Project 2")
                .workspacePath("/path/2")
                .build());

        assertEquals(2, projectRepository.findAll().size());
    }

    @Test
    void planAndApplyPersistTranslatedJavaAndEngineArtifacts() throws Exception {
        Files.writeString(workspacePath.resolve("routed.cob"), COBOL);
        Path output = workspacePath.resolve("generated");
        ProjectEntity project = projectRepository.save(ProjectEntity.builder()
                .name("Canonical generation")
                .workspacePath(workspacePath.toString())
                .branch("main")
                .build());
        Workspace workspace = new Workspace(project.getId(), workspacePath.toString(), "main");
        workspace.setMetadata(Map.of("outputDir", output.toString()));
        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("stubs");
        query.setLanguage("cobol");

        var plan = planService.createPlan(project.getId(), query, new Scope(List.of("**/*.cob")), workspace);
        assertThat(plan.isSuccess()).as(plan.getMessage()).isTrue();
        var apply = planService.applyPlan(project.getId(), plan.getPlanId(), false, workspace);

        assertThat(apply.isSuccess()).as(apply.getMessage()).isTrue();
        assertThat(apply.getModifiedFiles()).anyMatch(path -> path.endsWith("ServiceImpl.java"));
        assertThat(apply.getChanges())
                .containsEntry("javaOutputDirectory", output.toAbsolutePath().normalize().toString());
        assertThat((List<?>) apply.getChanges().get("javaGeneratedFiles"))
                .anyMatch(path -> path.toString().endsWith("ServiceImpl.java"));
        Path serviceImplementation;
        try (var files = Files.walk(output)) {
            serviceImplementation = files
                    .filter(path -> path.getFileName().toString().endsWith("ServiceImpl.java"))
                    .findFirst().orElseThrow();
        }
        assertThat(Files.readString(serviceImplementation))
                .contains("setCustomerName")
                .contains("System.out.println")
                .doesNotContain("// TODO: Implement COBOL business logic");
        assertThat(planRepository.findByPlanId(plan.getPlanId())).isPresent();
        assertThat(runRepository.findByRunId(apply.getRunId())).isPresent();
    }

    @Test
    void rejectsApplyWhenWorkspaceChangesAfterPlanning() throws Exception {
        Path source = workspacePath.resolve("routed.cob");
        Files.writeString(source, COBOL);
        ProjectEntity project = projectRepository.save(ProjectEntity.builder()
                .name("Stale plan")
                .workspacePath(workspacePath.toString())
                .branch("main")
                .build());
        Workspace workspace = new Workspace(project.getId(), workspacePath.toString(), "main");
        workspace.setMetadata(Map.of("outputDir", workspacePath.resolve("generated").toString()));
        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("stubs");
        query.setLanguage("cobol");

        var plan = planService.createPlan(project.getId(), query, new Scope(List.of("**/*.cob")), workspace);
        assertThat(plan.isSuccess()).as(plan.getMessage()).isTrue();
        Files.writeString(source, COBOL + "\n*> changed after plan\n");

        var apply = planService.applyPlan(project.getId(), plan.getPlanId(), false, workspace);

        assertThat(apply.isSuccess()).isFalse();
        assertThat(apply.getMessage()).contains("stale plan rejected");
        assertThat(runRepository.findAll()).isEmpty();
    }
}
