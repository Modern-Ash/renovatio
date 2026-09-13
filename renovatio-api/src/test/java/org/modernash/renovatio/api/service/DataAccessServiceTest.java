package org.modernash.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.modernash.renovatio.api.entity.JobEntity;
import org.modernash.renovatio.api.repository.JobRepository;
import org.modernash.renovatio.persistence.classifier.DataAccessClassifier;
import org.modernash.renovatio.persistence.registry.PersistenceStrategyRegistry;
import org.modernash.renovatio.profile.MigrationProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

class DataAccessServiceTest {
    private final JobRepository jobs = mock(JobRepository.class);
    private final DecisionLayerService decisions = mock(DecisionLayerService.class);
    private final DataAccessService service = new DataAccessService(
            new DataAccessClassifier(), new PersistenceStrategyRegistry(Set.of(),
                    org.modernash.renovatio.profile.MigrationProfile.PersistenceStrategy.IN_MEMORY), jobs,
            new ObjectMapper().findAndRegisterModules(), decisions);

    DataAccessServiceTest() {
        when(decisions.effective(anyString())).thenReturn(MigrationProfiles.effective(
                MigrationProfiles.emptyOverlay(), java.util.Map.of(), java.util.Map.of(), List.of()));
    }

    @Test
    void readsLatestCompletedAnalysisDataAccessesForProject() {
        JobEntity latest = JobEntity.builder().projectId("p1").operation("analyze")
                .status("COMPLETED")
                .resultJson("{\"dataAccesses\":[{\"id\":\"a1\",\"kind\":\"EXEC_SQL\",\"confidence\":1.0,\"suggestedStrategy\":\"JPA\",\"currentStrategy\":\"JPA\",\"keyShape\":{\"fields\":[]},\"recordShape\":{\"columns\":[]},\"discriminatorValues\":[]}]}")
                .build();
        when(jobs.findByProjectIdAndOperationAndStatusOrderByCompletedAtDesc("p1", "analyze", "COMPLETED"))
                .thenReturn(List.of(latest));

        assertEquals("a1", service.getClassifiedDataAccesses("p1").get(0).getId());
        assertEquals(1, service.getClassifiedDataAccesses("p1").size());
    }

    @Test
    void returnsEmptyWhenProjectHasNoCompletedAnalysis() {
        when(jobs.findByProjectIdAndOperationAndStatusOrderByCompletedAtDesc("p2", "analyze", "COMPLETED"))
                .thenReturn(List.of());

        assertEquals(List.of(), service.getClassifiedDataAccesses("p2"));
    }
}
