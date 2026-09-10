package org.shark.renovatio.api.repository;

import org.shark.renovatio.api.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
    List<JobEntity> findByProjectId(String projectId);
    List<JobEntity> findByProjectIdAndStatus(String projectId, String status);
    List<JobEntity> findByProjectIdOrderByCreatedAtDesc(String projectId);
    List<JobEntity> findByProjectIdAndOperationAndStatusOrderByCompletedAtDesc(
            String projectId, String operation, String status);
    List<JobEntity> findAllByOrderByCreatedAtDesc();
}
