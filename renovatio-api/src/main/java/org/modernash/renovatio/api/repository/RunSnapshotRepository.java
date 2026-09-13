package org.modernash.renovatio.api.repository;

import org.modernash.renovatio.api.entity.RunSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RunSnapshotRepository extends JpaRepository<RunSnapshotEntity, String> {
    Optional<RunSnapshotEntity> findByRunId(String runId);
    Optional<RunSnapshotEntity> findByProjectIdAndRunId(String projectId, String runId);
    List<RunSnapshotEntity> findByProjectIdOrderByStartedAtDesc(String projectId);
}
