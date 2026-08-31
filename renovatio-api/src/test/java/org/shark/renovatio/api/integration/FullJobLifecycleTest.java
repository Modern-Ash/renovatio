package org.shark.renovatio.api.integration;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.api.dto.ProjectDto;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.api.entity.ProjectEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class FullJobLifecycleTest {

    @Autowired
    private ProjectRepository projectRepository;

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
}
