package org.shark.renovatio.provider.cobol.polish;

import java.util.List;
import java.util.Map;

public record PolishPrerequisiteEvidence(
        String repositoryCommit,
        String baselineRef,
        List<String> characterizationSelectors,
        String characterizationCommand,
        String javaVersion,
        String mavenVersion,
        Map<String, String> generatedInputHashes,
        Map<String, String> semanticInputHashes,
        Map<String, String> expectedBehaviorHashes,
        boolean schemaGreen,
        boolean compilationGreen,
        boolean characterizationGreen,
        boolean transliterationStable,
        boolean unresolvedErrorItems) {

    public PolishPrerequisiteEvidence {
        repositoryCommit = PolishContracts.nonBlank(repositoryCommit, "repositoryCommit");
        baselineRef = PolishContracts.nonBlank(baselineRef, "baselineRef");
        if (characterizationSelectors == null || characterizationSelectors.isEmpty()
                || characterizationSelectors.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("At least one characterization selector is required");
        }
        characterizationSelectors = characterizationSelectors.stream().distinct().sorted().toList();
        characterizationCommand = PolishContracts.nonBlank(characterizationCommand, "characterizationCommand");
        javaVersion = PolishContracts.nonBlank(javaVersion, "javaVersion");
        mavenVersion = PolishContracts.nonBlank(mavenVersion, "mavenVersion");
        generatedInputHashes = PolishContracts.hashMap(generatedInputHashes, "generatedInputHashes");
        semanticInputHashes = PolishContracts.hashMap(semanticInputHashes, "semanticInputHashes");
        expectedBehaviorHashes = PolishContracts.hashMap(expectedBehaviorHashes, "expectedBehaviorHashes");
        if (!expectedBehaviorHashes.keySet().containsAll(characterizationSelectors)) {
            throw new IllegalArgumentException("Every characterization selector requires a behavior hash");
        }
    }

    public boolean isGreen() {
        return schemaGreen && compilationGreen && characterizationGreen && transliterationStable
                && !unresolvedErrorItems;
    }
}
