package org.modernash.renovatio.api.repository;

import org.modernash.renovatio.api.entity.ProjectDecisionEntity;
import org.modernash.renovatio.api.entity.ProjectDecisionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectDecisionRepository extends JpaRepository<ProjectDecisionEntity, ProjectDecisionId> {
    List<ProjectDecisionEntity> findAllByIdProjectId(String projectId);
    void deleteAllByIdProjectId(String projectId);
}
