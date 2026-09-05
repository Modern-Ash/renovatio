package org.shark.renovatio.api.service;

import org.shark.renovatio.api.config.EquivalenceRunnerProperties;
import org.shark.renovatio.domain.model.*;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@Service
public class EquivalenceReplayService {
 private final EquivalenceRunnerProperties properties;
 public EquivalenceReplayService(EquivalenceRunnerProperties properties) { this.properties = properties; }
 public EquivalenceFixture replay(String caseId, Map<String, ?> input, Set<String> ignoredFields) {
  if (properties.baselineCommand().isEmpty() || properties.candidateCommand().isEmpty()) throw new IllegalStateException("Equivalence runners are not configured");
  var baseline = new ProcessReplayRunner(properties.baselineCommand(), path(properties.baselineWorkingDirectory()));
  var candidate = new ProcessReplayRunner(properties.candidateCommand(), path(properties.candidateWorkingDirectory()));
  return new ReplayCoordinator(baseline, candidate).run(new ReplayRunner.ReplayInput(caseId, input), ignoredFields);
 }
 private static Path path(String value) { return value == null || value.isBlank() ? null : Path.of(value); }
}
