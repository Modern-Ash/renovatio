package org.shark.renovatio.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "renovatio.equivalence")
public record EquivalenceRunnerProperties(List<String> baselineCommand, String baselineWorkingDirectory,
                                          List<String> candidateCommand, String candidateWorkingDirectory) {
    public EquivalenceRunnerProperties {
        baselineCommand = baselineCommand == null ? List.of() : List.copyOf(baselineCommand);
        candidateCommand = candidateCommand == null ? List.of() : List.copyOf(candidateCommand);
        baselineWorkingDirectory = baselineWorkingDirectory == null ? "" : baselineWorkingDirectory;
        candidateWorkingDirectory = candidateWorkingDirectory == null ? "" : candidateWorkingDirectory;
    }
}
