package org.shark.renovatio.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.SuggestionRequest;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.DomainSuggestionDecisionRepository;
import org.shark.renovatio.api.repository.ProjectDomainModelVersionRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.domain.model.DomainModel.DomainNode;
import org.shark.renovatio.domain.model.DomainModel.Evidence;
import org.shark.renovatio.domain.model.DomainModel.Kind;
import org.shark.renovatio.domain.model.DomainModel.Origin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WorkbenchDomainModelServiceTest {
    @Autowired WorkbenchDomainModelService service;
    @Autowired ProjectRepository projects;
    @Autowired ProjectDomainModelVersionRepository versions;
    @Autowired DomainSuggestionDecisionRepository decisions;

    private String projectId;

    @BeforeEach
    void setUp() {
        decisions.deleteAll();
        versions.deleteAll();
        projects.deleteAll();
        projectId = projects.save(ProjectEntity.builder().name("Domain editor")
                .workspacePath("/tmp/domain-editor").branch("main").build()).getId();
    }

    @Test
    void versionsByHashComparesAndRestoresWithoutRewritingHistory() {
        assertThat(service.read(projectId).revision()).isZero();

        var first = service.save(projectId, 0, model(node("customer", "Customer", Origin.DETERMINISTIC)));
        assertThat(first.revision()).isEqualTo(1);
        assertThat(service.save(projectId, 1, first.model()).revision()).isEqualTo(1);

        DomainNode renamed = node("customer", "Account holder", Origin.HUMAN);
        var second = service.save(projectId, 1, model(renamed));
        assertThat(second.revision()).isEqualTo(2);
        assertThat(service.versions(projectId)).extracting(value -> value.revision()).containsExactly(2L, 1L);
        assertThat(service.compare(projectId, 1, 2).changed())
                .extracting(value -> value.targetId()).containsExactly("customer");

        var restored = service.restore(projectId, 1, 2);
        assertThat(restored.revision()).isEqualTo(3);
        assertThat(restored.model().nodes().get(0).name()).isEqualTo("Customer");
        assertThat(service.versions(projectId)).hasSize(3);
        assertThatThrownBy(() -> service.save(projectId, 1, model(renamed)))
                .isInstanceOf(WorkbenchDomainModelService.RevisionConflictException.class);
    }

    @Test
    void flagsMissingEvidenceAndRejectsCrossProjectSaves() {
        DomainNode unsupported = new DomainNode("orphan", Kind.ENTITY, "Orphan", List.of(),
                List.of(), Origin.HUMAN, 0.4);
        var saved = service.save(projectId, 0, model(unsupported));
        assertThat(saved.diagnostics()).extracting(value -> value.code())
                .containsExactlyInAnyOrder("MISSING_EVIDENCE", "LOW_CONFIDENCE");

        assertThatThrownBy(() -> service.save(projectId, 1,
                new DomainModel("1", "another-project", List.of(), List.of(), List.of())))
                .isInstanceOf(WorkbenchDomainModelService.ValidationException.class);
    }

    @Test
    void suggestionDecisionsAreExplicitIdempotentAndRevisionAware() {
        DomainNode suggestion = node("customer", "Suggested customer", Origin.LLM);
        service.save(projectId, 0, model(suggestion));
        assertThat(service.read(projectId).suggestions()).extracting(value -> value.status())
                .containsExactly("pending");

        var accepted = service.decide(projectId, "node:customer",
                new SuggestionRequest("accepted", 1, null, null));
        assertThat(accepted.suggestions()).extracting(value -> value.status()).containsExactly("accepted");
        assertThat(service.decide(projectId, "node:customer",
                new SuggestionRequest("accepted", 1, null, null)).revision()).isEqualTo(1);
        assertThatThrownBy(() -> service.decide(projectId, "node:customer",
                new SuggestionRequest("rejected", 1, null, null)))
                .isInstanceOf(WorkbenchDomainModelService.RevisionConflictException.class);
    }

    @Test
    void editingSuggestionCreatesHumanVersionAndRejectingReferencedNodeFails() {
        DomainNode suggestion = node("customer", "Suggested customer", Origin.LLM);
        var account = node("account", "Account", Origin.DETERMINISTIC);
        DomainModel referenced = new DomainModel("1", projectId, List.of(suggestion, account),
                List.of(new DomainModel.DomainRelation("owns", "account", "customer",
                        DomainModel.RelationKind.CONTAINS)), List.of());
        service.save(projectId, 0, referenced);
        assertThatThrownBy(() -> service.decide(projectId, "node:customer",
                new SuggestionRequest("rejected", 1, null, null)))
                .isInstanceOf(WorkbenchDomainModelService.ValidationException.class);

        DomainNode edited = node("customer", "Customer", Origin.LLM);
        var result = service.decide(projectId, "node:customer",
                new SuggestionRequest("edited", 1, edited, null));
        assertThat(result.revision()).isEqualTo(2);
        assertThat(result.model().nodes().stream().filter(node -> node.id().equals("customer"))
                .findFirst().orElseThrow().origin()).isEqualTo(Origin.HUMAN);
        assertThat(result.suggestions()).isEmpty();
    }

    private DomainModel model(DomainNode... nodes) {
        return new DomainModel("1", projectId, List.of(nodes), List.of(), List.of());
    }

    private DomainNode node(String id, String name, Origin origin) {
        return new DomainNode(id, Kind.ENTITY, name, List.of(),
                List.of(new Evidence("src/" + id + ".cbl:1", "a".repeat(64), "fixture")), origin, 0.9);
    }
}
