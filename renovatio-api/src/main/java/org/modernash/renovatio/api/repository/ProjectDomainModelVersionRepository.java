package org.modernash.renovatio.api.repository;

import java.util.List;
import java.util.Optional;
import org.modernash.renovatio.api.entity.ProjectDomainModelVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectDomainModelVersionRepository extends JpaRepository<ProjectDomainModelVersionEntity, String> {
    Optional<ProjectDomainModelVersionEntity> findFirstByProjectIdOrderByRevisionDesc(String projectId);
    Optional<ProjectDomainModelVersionEntity> findByProjectIdAndRevision(String projectId, long revision);
    List<ProjectDomainModelVersionEntity> findByProjectIdOrderByRevisionDesc(String projectId);
}
