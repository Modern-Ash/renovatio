package org.shark.renovatio.api.repository;

import java.util.List;
import java.util.Optional;
import org.shark.renovatio.api.entity.ProjectArchitectureProfileVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectArchitectureProfileVersionRepository
        extends JpaRepository<ProjectArchitectureProfileVersionEntity, String> {
    Optional<ProjectArchitectureProfileVersionEntity> findFirstByProjectIdOrderByRevisionDesc(String projectId);
    Optional<ProjectArchitectureProfileVersionEntity> findByProjectIdAndRevision(String projectId, long revision);
    List<ProjectArchitectureProfileVersionEntity> findByProjectIdOrderByRevisionDesc(String projectId);
}
