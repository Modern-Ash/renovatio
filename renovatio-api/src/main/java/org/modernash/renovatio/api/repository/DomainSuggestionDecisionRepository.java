package org.modernash.renovatio.api.repository;

import java.util.List;
import java.util.Optional;
import org.modernash.renovatio.api.entity.DomainSuggestionDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainSuggestionDecisionRepository extends JpaRepository<DomainSuggestionDecisionEntity, String> {
    Optional<DomainSuggestionDecisionEntity> findByProjectIdAndSuggestionId(String projectId, String suggestionId);
    List<DomainSuggestionDecisionEntity> findByProjectId(String projectId);
}
