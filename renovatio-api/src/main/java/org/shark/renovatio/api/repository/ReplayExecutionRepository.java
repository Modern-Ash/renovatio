package org.shark.renovatio.api.repository;
import org.shark.renovatio.api.entity.ReplayExecutionEntity; import org.springframework.data.jpa.repository.JpaRepository;
public interface ReplayExecutionRepository extends JpaRepository<ReplayExecutionEntity,Long>{
 java.util.List<ReplayExecutionEntity> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
