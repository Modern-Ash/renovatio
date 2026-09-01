package org.shark.renovatio.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.api.entity.ProjectDecisionEntity;
import org.shark.renovatio.api.entity.ProjectDecisionId;
import org.shark.renovatio.api.repository.ProjectDecisionRepository;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.decisions.DecisionStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class JpaDecisionStore implements DecisionStore {
    private final ProjectDecisionRepository repository;
    private final ObjectMapper json;
    public JpaDecisionStore(ProjectDecisionRepository repository, ObjectMapper json) {
        this.repository = repository; this.json = json;
    }

    @Override public List<DecisionPoint> findAll(String projectId) {
        return repository.findAllByIdProjectId(projectId).stream().map(this::decode)
                .sorted(DecisionResolver.apiOrder()).toList();
    }

    @Override public List<DecisionPoint> find(String projectId, DecisionPoint.Category category,
                                              BigDecimal minConfidence, DecisionPoint.Status status) {
        return findAll(projectId).stream()
                .filter(DecisionPoint::active)
                .filter(value -> category == null || value.category() == category)
                .filter(value -> minConfidence == null || value.confidence().compareTo(minConfidence) >= 0)
                .filter(value -> status == null || value.status() == status).toList();
    }
    @Override public Optional<DecisionPoint> findById(String projectId, String decisionId) {
        return repository.findById(new ProjectDecisionId(projectId, decisionId)).map(this::decode);
    }
    @Override public DecisionPoint save(String projectId, DecisionPoint decision) {
        ProjectDecisionId id = new ProjectDecisionId(projectId, decision.id());
        ProjectDecisionEntity entity = repository.findById(id).orElseGet(() -> new ProjectDecisionEntity(id));
        entity.update(decision.category().name(), decision.decisionKey(), decision.status().name(),
                decision.confidence(), decision.active(), encode(decision));
        return decode(repository.saveAndFlush(entity));
    }
    @Override public List<DecisionPoint> saveAll(String projectId, List<DecisionPoint> decisions) {
        return decisions.stream().map(value -> save(projectId, value)).toList();
    }
    @Override public void deleteProject(String projectId) { repository.deleteAllByIdProjectId(projectId); }

    private String encode(DecisionPoint value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot persist decision", exception); }
    }
    private DecisionPoint decode(ProjectDecisionEntity value) {
        try { return json.readValue(value.getDecisionJson(), DecisionPoint.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot read decision", exception); }
    }
}
