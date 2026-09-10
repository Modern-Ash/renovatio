package org.shark.renovatio.api.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.shark.renovatio.api.dto.WorkbenchContextDto;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import java.util.Optional;
import static org.mockito.Mockito.*;

class WorkbenchContextServiceTest {
    @Test
    void persistsOnlyValidProjectScopedContext() {
        ProjectRepository repository = mock(ProjectRepository.class);
        ProjectEntity project = new ProjectEntity(); project.setId("p1");
        when(repository.findById("p1")).thenReturn(Optional.of(project));
        WorkbenchContextService service = new WorkbenchContextService(repository);
        assertEquals("analysis", service.save("p1", new WorkbenchContextDto("analysis", "runs/latest.json")).activeArea());
        assertEquals("runs/latest.json", project.getWorkbenchSelectedAssetId());
        assertThrows(IllegalArgumentException.class, () -> service.save("p1", new WorkbenchContextDto("unknown", "asset")));
        assertThrows(IllegalArgumentException.class, () -> service.save("p1", new WorkbenchContextDto("project", "../secret")));
    }

    @Test
    void dropsMalformedStoredValuesOnRead() {
        ProjectRepository repository = mock(ProjectRepository.class);
        ProjectEntity project = new ProjectEntity(); project.setId("p1"); project.setWorkbenchActiveArea("project"); project.setWorkbenchSelectedAssetId("../secret");
        when(repository.findById("p1")).thenReturn(Optional.of(project));
        WorkbenchContextDto context = new WorkbenchContextService(repository).read("p1");
        assertNull(context.activeArea()); assertNull(context.selectedAssetId());
    }

    @Test
    void returnsAnEmptyContextWhenNothingHasBeenStored() {
        ProjectRepository repository = mock(ProjectRepository.class);
        ProjectEntity project = new ProjectEntity(); project.setId("p1");
        when(repository.findById("p1")).thenReturn(Optional.of(project));
        WorkbenchContextDto context = new WorkbenchContextService(repository).read("p1");
        assertNull(context.activeArea()); assertNull(context.selectedAssetId());
    }
}
