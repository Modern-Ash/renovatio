package org.shark.renovatio.decisions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Project-scoped persistence port; adapters must never query a decision id without project id. */
public interface DecisionStore {
    List<DecisionPoint> find(String projectId, DecisionPoint.Category category,
                             BigDecimal minConfidence, DecisionPoint.Status status);
    Optional<DecisionPoint> findById(String projectId, String decisionId);
    DecisionPoint save(String projectId, DecisionPoint decision);
    List<DecisionPoint> saveAll(String projectId, List<DecisionPoint> decisions);
    void deleteProject(String projectId);
}
