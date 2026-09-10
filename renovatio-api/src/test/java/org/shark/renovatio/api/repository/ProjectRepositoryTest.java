package org.shark.renovatio.api.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.shark.renovatio.api.entity.ProjectEntity;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void shouldSaveAndRetrieveProject() {
        ProjectEntity project = ProjectEntity.builder()
                .name("Test Project")
                .workspacePath("/path/to/workspace")
                .branch("main")
                .build();

        ProjectEntity saved = projectRepository.save(project);
        assertNotNull(saved.getId());

        ProjectEntity retrieved = projectRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals("Test Project", retrieved.getName());
        assertEquals("/path/to/workspace", retrieved.getWorkspacePath());
        assertEquals("main", retrieved.getBranch());
        assertNotNull(retrieved.getCreatedAt());
    }

    @Test
    void shouldListAllProjects() {
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
}
