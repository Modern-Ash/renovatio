package org.shark.renovatio.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.ArchitectureProfileDraft;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.DependencyRule;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.ExcludedNode;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto.LayoutPosition;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectArchitectureProfileVersionRepository;
import org.shark.renovatio.api.repository.ProjectDecisionRepository;
import org.shark.renovatio.api.repository.ProjectDomainModelVersionRepository;
import org.shark.renovatio.api.repository.ProjectProfileRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.profile.MigrationProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WorkbenchArchitectureCanvasServiceTest {
    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. CANVAS.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(20).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    @Autowired WorkbenchArchitectureCanvasService service;
    @Autowired WorkbenchDomainModelService domainModels;
    @Autowired ProjectRepository projects;
    @Autowired ProjectArchitectureProfileVersionRepository architectureVersions;
    @Autowired ProjectDomainModelVersionRepository domainVersions;
    @Autowired ProjectProfileRepository profiles;
    @Autowired ProjectDecisionRepository decisions;
    @TempDir Path workspace;

    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        architectureVersions.deleteAll();
        domainVersions.deleteAll();
        decisions.deleteAll();
        profiles.deleteAll();
        projects.deleteAll();
        Files.writeString(workspace.resolve("canvas.cob"), COBOL);
        projectId = projects.save(ProjectEntity.builder().name("Architecture canvas")
                .workspacePath(workspace.toString()).branch("main").build()).getId();
    }

    @Test
    void mvcDefaultsExposeModelServiceControllerAndDoNotMutateDomainModel() {
        var view = service.read(projectId);

        assertThat(view.profile().style()).isEqualTo(MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT);
        var saved = service.save(projectId, 0, new ArchitectureProfileDraft(
                MigrationProfile.ArchitectureStyle.LAYERED_MVC, MigrationProfile.ModuleGrouping.BY_PROGRAM,
                MigrationProfile.Framework.SPRING_BOOT, MigrationProfile.PersistenceStrategy.IN_MEMORY,
                Map.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of()));

        assertThat(saved.revision()).isEqualTo(1);
        assertThat(saved.canvas()).extracting(value -> value.layer())
                .contains("model", "service", "controller");
        assertThat(saved.manifest()).extracting(value -> value.path())
                .anyMatch(path -> path.contains("/model/"))
                .anyMatch(path -> path.contains("/service/"));
        assertThat(domainModels.read(projectId).revision()).isZero();
        assertThat(domainVersions.findByProjectIdOrderByRevisionDesc(projectId)).isEmpty();
    }

    @Test
    void customNamingRecomputesShadowAndDependencyDiagnosticsBeforeGeneration() {
        var saved = service.save(projectId, 0, new ArchitectureProfileDraft(
                MigrationProfile.ArchitectureStyle.LAYERED_MVC, MigrationProfile.ModuleGrouping.BY_PROGRAM,
                MigrationProfile.Framework.SPRING_BOOT, MigrationProfile.PersistenceStrategy.JPA,
                Map.of("service", "com.acme.app.services", "model", "com.acme.app.domain"),
                Map.of("service", "UseCase", "model", "Record"), Map.of(),
                List.of(new DependencyRule("service", "model", false, "force a visible violation")),
                Map.of(), List.of()));

        assertThat(saved.dependencyDiagnostics()).extracting(value -> value.code())
                .contains("FORBIDDEN_DEPENDENCY");
        assertThat(saved.manifest()).extracting(value -> value.path())
                .anyMatch(path -> path.startsWith("com/acme/app/domain/") && path.endsWith("Record.java"))
                .anyMatch(path -> path.startsWith("com/acme/app/services/") && path.endsWith("UseCase.java"));
        assertThat(workspace.resolve("generated-java-stubs")).doesNotExist();
    }

    @Test
    void versionedProfilesCompareRestoreAndRejectStaleWrites() {
        var first = service.save(projectId, 0, new ArchitectureProfileDraft(
                MigrationProfile.ArchitectureStyle.LAYERED_MVC, MigrationProfile.ModuleGrouping.BY_PROGRAM,
                MigrationProfile.Framework.SPRING_BOOT, MigrationProfile.PersistenceStrategy.IN_MEMORY,
                Map.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of()));
        var second = service.save(projectId, 1, new ArchitectureProfileDraft(
                MigrationProfile.ArchitectureStyle.HEXAGONAL, MigrationProfile.ModuleGrouping.BY_DOMAIN,
                MigrationProfile.Framework.SPRING_BOOT, MigrationProfile.PersistenceStrategy.JPA,
                Map.of("adapter", "com.acme.adapters"), Map.of("adapter", "Gateway"), Map.of(), List.of(),
                Map.of(), List.of()));

        assertThat(first.canonicalHash()).startsWith("sha256:");
        assertThat(second.canonicalHash()).isNotEqualTo(first.canonicalHash());
        assertThat(service.versions(projectId)).extracting(value -> value.revision()).containsExactly(2L, 1L);
        assertThat(service.compare(projectId, 1, 2).changed()).extracting(value -> value.targetId())
                .contains("style", "moduleGrouping", "persistence");

        var restored = service.restore(projectId, 1, 2);
        assertThat(restored.revision()).isEqualTo(3);
        assertThat(restored.profile().style()).isEqualTo(MigrationProfile.ArchitectureStyle.LAYERED_MVC);
        assertThatThrownBy(() -> service.save(projectId, 1, second.profile()))
                .isInstanceOf(WorkbenchArchitectureCanvasService.RevisionConflictException.class);
    }

    @Test
    void layoutAndPruneMetadataRoundTripThroughVersionedArchitectureProfile() {
        var base = service.save(projectId, 0, new ArchitectureProfileDraft(
                MigrationProfile.ArchitectureStyle.LAYERED_MVC, MigrationProfile.ModuleGrouping.BY_PROGRAM,
                MigrationProfile.Framework.SPRING_BOOT, MigrationProfile.PersistenceStrategy.IN_MEMORY,
                Map.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of()));
        String nodeId = base.canvas().get(0).id();
        var arranged = service.save(projectId, 1, new ArchitectureProfileDraft(
                base.profile().style(), base.profile().moduleGrouping(), base.profile().framework(),
                base.profile().persistence(), base.profile().packageRoots(), base.profile().suffixes(),
                base.profile().classNames(), base.profile().dependencyRules(),
                Map.of(nodeId, new LayoutPosition(320, 180)),
                List.of(new ExcludedNode(nodeId, "Legacy report only"))));

        assertThat(arranged.profile().layout()).containsEntry(nodeId, new LayoutPosition(320, 180));
        assertThat(arranged.profile().excludedNodeIds()).containsExactly(new ExcludedNode(nodeId, "Legacy report only"));
        assertThat(arranged.canvas().stream().filter(node -> node.id().equals(nodeId)).findFirst().orElseThrow().excluded())
                .isTrue();
        assertThat(service.compare(projectId, 1, 2).added()).extracting(value -> value.targetId())
                .contains("layout." + nodeId, "excludedNodeIds." + nodeId);
    }

    @Test
    void generateChangeSetRequestFiltersExcludedNodesAndRejectsBlockingDiagnostics() {
        var base = service.save(projectId, 0, new ArchitectureProfileDraft(
                MigrationProfile.ArchitectureStyle.LAYERED_MVC, MigrationProfile.ModuleGrouping.BY_PROGRAM,
                MigrationProfile.Framework.SPRING_BOOT, MigrationProfile.PersistenceStrategy.IN_MEMORY,
                Map.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of()));
        String excludedId = base.canvas().get(0).componentId();
        var pruned = service.save(projectId, 1, new ArchitectureProfileDraft(
                base.profile().style(), base.profile().moduleGrouping(), base.profile().framework(),
                base.profile().persistence(), base.profile().packageRoots(), base.profile().suffixes(),
                base.profile().classNames(), base.profile().dependencyRules(), Map.of(),
                List.of(new ExcludedNode(excludedId, "Pilot excluded"))));

        var request = service.generateChangeSetRequest(projectId);

        assertThat(request.title()).contains("architecture revision " + pruned.revision());
        assertThat(request.files()).noneMatch(file -> file.proposedContent().contains("component: " + excludedId));
        assertThat(request.evidence()).contains("excludedNodeIds:1");

        service.save(projectId, 2, new ArchitectureProfileDraft(
                MigrationProfile.ArchitectureStyle.LAYERED_MVC, MigrationProfile.ModuleGrouping.BY_PROGRAM,
                MigrationProfile.Framework.SPRING_BOOT, MigrationProfile.PersistenceStrategy.IN_MEMORY,
                Map.of(), Map.of(), Map.of(),
                List.of(new DependencyRule("service", "model", false, "block generate")), Map.of(), List.of()));
        assertThatThrownBy(() -> service.generateChangeSetRequest(projectId))
                .isInstanceOf(WorkbenchArchitectureCanvasService.ValidationException.class);
    }
}
