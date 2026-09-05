package org.shark.renovatio.api.service;

import org.shark.renovatio.api.config.EquivalenceRunnerProperties;
import org.shark.renovatio.domain.model.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.api.entity.ReplayExecutionEntity;
import org.shark.renovatio.api.repository.ReplayExecutionRepository;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@Service
public class EquivalenceReplayService {
 private final EquivalenceRunnerProperties properties; private final ReplayExecutionRepository history; private final ObjectMapper mapper;
 public EquivalenceReplayService(EquivalenceRunnerProperties properties, ReplayExecutionRepository history, ObjectMapper mapper) { this.properties = properties; this.history = history; this.mapper = mapper; }
 public EquivalenceFixture replay(String projectId, String caseId, Map<String, ?> input, Set<String> ignoredFields) {
  if (properties.baselineCommand().isEmpty() || properties.candidateCommand().isEmpty()) throw new IllegalStateException("Equivalence runners are not configured");
  var baseline = new ProcessReplayRunner(properties.baselineCommand(), path(properties.baselineWorkingDirectory()));
  var candidate = new ProcessReplayRunner(properties.candidateCommand(), path(properties.candidateWorkingDirectory()));
  var fixture = new ReplayCoordinator(baseline, candidate).run(new ReplayRunner.ReplayInput(caseId, input), ignoredFields);
  try { history.save(new ReplayExecutionEntity(projectId, caseId, mapper.writeValueAsString(fixture), fixture.compare().equivalent())); } catch (Exception e) { throw new IllegalStateException("Unable to persist replay", e); }
  return fixture;
 }
 private static Path path(String value) { return value == null || value.isBlank() ? null : Path.of(value); }
}
