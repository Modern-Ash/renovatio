package org.shark.renovatio.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.dto.ProjectDto;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.shared.security.WorkspaceRootPolicy;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectServiceWorkspaceSecurityTest {
    @TempDir Path temp;

    @Test
    void createsProjectsOnlyUnderConfiguredWorkspaceRoots() throws Exception {
        ProjectRepository projects = mock(ProjectRepository.class);
        when(projects.save(any(ProjectEntity.class))).thenAnswer(invocation -> {
            ProjectEntity entity = invocation.getArgument(0);
            entity.setId("p1");
            return entity;
        });
        when(projects.findById("p1")).thenAnswer(invocation -> java.util.Optional.of(ProjectEntity.builder()
                .id("p1")
                .name("Payroll")
                .workspacePath(temp.resolve("allowed").resolve("payroll").toRealPath().toString())
                .javaOutputPath(temp.resolve("allowed").resolve("payroll").resolve("generated-java-stubs").toString())
                .build()));
        ProjectService service = new ProjectService(projects, mock(DecisionLayerService.class),
                mock(ReusableAssetsService.class), WorkspaceRootPolicy.under(temp.resolve("allowed")));

        ProjectDto created = service.createProject(ProjectDto.builder()
                .name("Payroll")
                .workspacePath("payroll")
                .build());

        assertTrue(Path.of(created.getWorkspacePath()).startsWith(temp.resolve("allowed").toRealPath()));
        assertThrows(SecurityException.class, () -> service.createProject(ProjectDto.builder()
                .name("Escape")
                .workspacePath(temp.resolve("outside").toString())
                .build()));
    }
}
