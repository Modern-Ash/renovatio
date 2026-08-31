package org.shark.renovatio.provider.cobol.polish;

import java.util.Map;
import java.util.Set;

public record PolishCandidate(
        String unifiedDiff,
        Set<String> changedPaths,
        Map<String, String> outputHashes,
        Set<String> publicSignatureChanges,
        Set<String> ownerApprovedSignatures,
        Map<String, String> provenance,
        PolishFamilyPayload familyPayload,
        boolean byteReproducible) {

    private static final Set<String> REQUIRED_PROVENANCE = Set.of(
            "promptId", "promptVersion", "promptHash", "outputSchemaHash", "validators",
            "cacheKey", "cacheHash", "providerId", "modelId", "agoraToolRun",
            "resultDisposition");

    public PolishCandidate {
        unifiedDiff = PolishContracts.nonBlank(unifiedDiff, "unifiedDiff");
        if (unifiedDiff.indexOf('\r') >= 0 || !unifiedDiff.endsWith("\n")) {
            throw new IllegalArgumentException("unifiedDiff must use LF and end with a newline");
        }
        changedPaths = PolishContracts.javaPaths(changedPaths, "changedPaths");
        outputHashes = PolishContracts.hashMap(outputHashes, "outputHashes");
        publicSignatureChanges = PolishContracts.nonBlankSetOrEmpty(
                publicSignatureChanges, "publicSignatureChanges");
        ownerApprovedSignatures = PolishContracts.nonBlankSetOrEmpty(
                ownerApprovedSignatures, "ownerApprovedSignatures");
        provenance = PolishContracts.stringMap(provenance, "provenance");
        if (familyPayload == null) throw new IllegalArgumentException("familyPayload is required");
        if (!provenance.keySet().containsAll(REQUIRED_PROVENANCE)
                || !"MODEL_SUCCESS".equals(provenance.get("resultDisposition"))
                || !familyPayload.family().promptId().equals(provenance.get("promptId"))
                || !"1".equals(provenance.get("promptVersion"))) {
            throw new IllegalArgumentException("Complete successful governed provenance is required");
        }
        PolishContracts.hash(provenance.get("promptHash"), "promptHash");
        PolishContracts.hash(provenance.get("outputSchemaHash"), "outputSchemaHash");
        PolishContracts.hash(provenance.get("cacheKey"), "cacheKey");
        PolishContracts.hash(provenance.get("cacheHash"), "cacheHash");
        if (!outputHashes.keySet().equals(changedPaths)) {
            throw new IllegalArgumentException("Output hashes must exactly match changed paths");
        }
    }
}
