package org.modernash.renovatio.api.repository;

import org.modernash.renovatio.api.entity.MigrationPlanSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MigrationPlanSnapshotRepository extends JpaRepository<MigrationPlanSnapshotEntity, String> {
    Optional<MigrationPlanSnapshotEntity> findByPlanId(String planId);
    Optional<MigrationPlanSnapshotEntity> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
