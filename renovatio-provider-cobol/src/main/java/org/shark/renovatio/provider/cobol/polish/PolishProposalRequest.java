package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record PolishProposalRequest(
        PolishProposalFamily family,
        String sourceFile,
        String program,
        String sourceContentHash,
        String generatedRoot,
        Map<String, String> generatedSources,
        Map<String, JsonNode> semanticProjections,
        Map<String, String> pathSelectors,
        Map<String, String> nodeSelectors,
        PolishPrerequisiteEvidence evidence) {

    public PolishProposalRequest {
        if (family == null) throw new IllegalArgumentException("family is required");
        sourceFile = PolishContracts.nonBlank(sourceFile, "sourceFile");
        program = PolishContracts.nonBlank(program, "program");
        sourceContentHash = PolishContracts.hash(sourceContentHash, "sourceContentHash");
        generatedRoot = PolishContracts.relativeDirectory(generatedRoot, "generatedRoot");
        if (generatedSources == null || generatedSources.isEmpty()) {
            throw new IllegalArgumentException("generatedSources must not be empty");
        }
        TreeMap<String, String> sources = new TreeMap<>();
        generatedSources.forEach((path, source) -> sources.put(
                PolishContracts.javaPath(path), PolishContracts.nonBlank(source, "generated source")));
        generatedSources = Collections.unmodifiableMap(sources);
        if (evidence == null) throw new IllegalArgumentException("evidence is required");
        semanticProjections = semanticProjections(semanticProjections);
        pathSelectors = selectorMap(pathSelectors, true, evidence);
        nodeSelectors = selectorMap(nodeSelectors, false, evidence);
        if (!pathSelectors.keySet().containsAll(generatedSources.keySet())) {
            throw new IllegalArgumentException("Every generated Java input requires a characterization selector");
        }
        if (!evidence.generatedInputHashes().keySet().equals(generatedSources.keySet())
                || generatedSources.entrySet().stream().anyMatch(entry -> !PolishContracts.sha256(entry.getValue())
                .equals(evidence.generatedInputHashes().get(entry.getKey())))) {
            throw new IllegalArgumentException("Evidence must exactly hash every generated Java input");
        }
        if (!semanticProjections.keySet().equals(nodeSelectors.keySet())
                || !evidence.semanticInputHashes().keySet().equals(semanticProjections.keySet())
                || semanticProjections.entrySet().stream().anyMatch(entry ->
                !PolishContracts.canonicalJsonHash(entry.getValue())
                        .equals(evidence.semanticInputHashes().get(entry.getKey())))) {
            throw new IllegalArgumentException("Evidence must exactly hash every affected semantic projection");
        }
    }

    private static Map<String, JsonNode> semanticProjections(Map<String, JsonNode> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("semanticProjections must not be empty");
        }
        TreeMap<String, JsonNode> result = new TreeMap<>();
        values.forEach((nodeId, projection) -> {
            if (projection == null || !projection.isObject()) {
                throw new IllegalArgumentException("Semantic projections must be JSON objects");
            }
            result.put(PolishContracts.nonBlank(nodeId, "semantic projection node ID"),
                    projection.deepCopy());
        });
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> selectorMap(Map<String, String> values, boolean paths,
                                                   PolishPrerequisiteEvidence evidence) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException((paths ? "pathSelectors" : "nodeSelectors")
                    + " must not be empty");
        }
        TreeMap<String, String> result = new TreeMap<>();
        values.forEach((key, selector) -> {
            String normalizedKey = paths
                    ? PolishContracts.javaPath(key)
                    : PolishContracts.nonBlank(key, "affected node ID");
            String normalizedSelector = PolishContracts.nonBlank(selector, "characterization selector");
            if (!evidence.characterizationSelectors().contains(normalizedSelector)) {
                throw new IllegalArgumentException("Every affected path and node must map to a named selector");
            }
            result.put(normalizedKey, normalizedSelector);
        });
        return Collections.unmodifiableMap(result);
    }
}
