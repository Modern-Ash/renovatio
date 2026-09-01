package org.shark.renovatio.api.repository;

import org.shark.renovatio.api.entity.RunSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RunSnapshotRepository extends JpaRepository<RunSnapshotEntity, String> {
    Optional<RunSnapshotEntity> findByRunId(String runId);
    Optional<RunSnapshotEntity> findByProjectIdAndRunId(String projectId, String runId);
}
