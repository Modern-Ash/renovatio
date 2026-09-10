package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.JsonNode;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record PolishProposalManifest(
        String schemaVersion,
        String proposalId,
        PolishProposalFamily family,
        String reviewState,
        PolishDisposition disposition,
        String repositoryCommit,
        String baselineRef,
        List<String> characterizationSelectors,
        String characterizationCommand,
        String javaVersion,
        String mavenVersion,
        Map<String, String> generatedInputHashes,
        Map<String, String> semanticInputHashes,
        Map<String, String> expectedBehaviorHashes,
        Map<String, String> pathSelectors,
        Map<String, String> nodeSelectors,
        Map<String, String> outputHashes,
        String generatedTreeHash,
        Set<String> changedPaths,
        String patchHash,
        Map<String, String> provenance,
        Set<String> publicSignatureChanges,
        List<GuardrailGate> executedGates,
        String diagnosticReference,
        JsonNode familyPayload) {

    public static final String SCHEMA_VERSION = "idiomatic-polish-proposal.v1";

    public PolishProposalManifest {
        schemaVersion = PolishContracts.nonBlank(schemaVersion, "schemaVersion");
        proposalId = PolishContracts.nonBlank(proposalId, "proposalId");
        reviewState = PolishContracts.nonBlank(reviewState, "reviewState");
        repositoryCommit = PolishContracts.nonBlank(repositoryCommit, "repositoryCommit");
        baselineRef = PolishContracts.nonBlank(baselineRef, "baselineRef");
        characterizationSelectors = List.copyOf(characterizationSelectors);
        characterizationCommand = PolishContracts.nonBlank(characterizationCommand, "characterizationCommand");
        javaVersion = PolishContracts.nonBlank(javaVersion, "javaVersion");
        mavenVersion = PolishContracts.nonBlank(mavenVersion, "mavenVersion");
        generatedInputHashes = PolishContracts.hashMap(generatedInputHashes, "generatedInputHashes");
        semanticInputHashes = PolishContracts.hashMap(semanticInputHashes, "semanticInputHashes");
        expectedBehaviorHashes = PolishContracts.hashMap(expectedBehaviorHashes, "expectedBehaviorHashes");
        pathSelectors = PolishContracts.stringMap(pathSelectors, "pathSelectors");
        nodeSelectors = PolishContracts.stringMap(nodeSelectors, "nodeSelectors");
        outputHashes = PolishContracts.hashMap(outputHashes, "outputHashes");
        generatedTreeHash = PolishContracts.hash(generatedTreeHash, "generatedTreeHash");
        changedPaths = PolishContracts.javaPaths(changedPaths, "changedPaths");
        patchHash = PolishContracts.hash(patchHash, "patchHash");
        provenance = PolishContracts.stringMap(provenance, "provenance");
        publicSignatureChanges = PolishContracts.nonBlankSetOrEmpty(
                publicSignatureChanges, "publicSignatureChanges");
        executedGates = List.copyOf(executedGates);
        diagnosticReference = PolishContracts.nonBlank(diagnosticReference, "diagnosticReference");
        if (family == null || disposition == null || familyPayload == null) {
            throw new IllegalArgumentException("family, disposition and familyPayload are required");
        }
        if (!SCHEMA_VERSION.equals(schemaVersion) || !"PROPOSED".equals(reviewState)
                || disposition != PolishDisposition.ELIGIBLE_FOR_REVIEW) {
            throw new IllegalArgumentException("Retained manifests must remain review-only proposals");
        }
    }
}
