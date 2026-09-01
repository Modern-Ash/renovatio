package org.shark.renovatio.api.repository;

import org.shark.renovatio.api.entity.ActionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItemEntity, String> {
    List<ActionItemEntity> findByProjectId(String projectId);
    List<ActionItemEntity> findByProjectIdAndReviewStatus(String projectId, String reviewStatus);
    List<ActionItemEntity> findByRunId(String runId);
}
