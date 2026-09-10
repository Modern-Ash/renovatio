package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.shark.renovatio.provider.cobol.guardrail.GateCheckResult;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailSchemaCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PolishCandidateValidator {

    private static final Pattern HUNK_HEADER = Pattern.compile(
            "@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*");
    private final ObjectMapper objectMapper;
    private final JsonSchema schema;

    PolishCandidateValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(new GuardrailSchemaCatalog(objectMapper)
                        .resolve(PolishProposalManifest.SCHEMA_VERSION));
    }

    GateCheckResult validate(PolishProposalRequest request, PolishCandidate candidate,
                             PolishProposalManifest manifest) {
        try {
            if (candidate.familyPayload().family() != request.family()) {
                return GateCheckResult.failed("schema:family-mismatch");
            }
            Set<String> diffPaths = diffPaths(candidate.unifiedDiff());
            if (!diffPaths.equals(candidate.changedPaths())) {
                return GateCheckResult.failed("schema:diff-path-mismatch");
            }
            if (!request.pathSelectors().keySet().containsAll(candidate.changedPaths())) {
                return GateCheckResult.failed("schema:undeclared-changed-path");
            }

            Map<String, String> proposedOutputHashes = proposedOutputHashes(
                    request.generatedSources(), candidate.unifiedDiff());
            for (Map.Entry<String, String> output : candidate.outputHashes().entrySet()) {
                String actual = proposedOutputHashes.get(output.getKey());
                if (actual == null || !actual.equals(output.getValue())) {
                    return GateCheckResult.failed("schema:output-hash-mismatch");
                }
            }

            GateCheckResult familyBoundary = familyBoundary(request, candidate.familyPayload());
            if (!familyBoundary.passed()) {
                return familyBoundary;
            }
            if (!schema.validate(objectMapper.valueToTree(manifest)).isEmpty()) {
                return GateCheckResult.failed("schema:manifest-invalid");
            }
            return GateCheckResult.passed("schema:valid");
        } catch (RuntimeException exception) {
            return GateCheckResult.failed("schema:candidate-invalid");
        }
    }

    private Map<String, String> proposedOutputHashes(Map<String, String> generatedSources,
                                                     String diff) {
        Map<String, String> proposedOutputHashes = new LinkedHashMap<>();
        List<String> lines = lines(diff);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (!line.startsWith("--- ")) {
                continue;
            }
            if (index + 1 >= lines.size() || !lines.get(index + 1).startsWith("+++ ")) {
                throw new IllegalArgumentException("Incomplete unified diff header");
            }

            String oldPath = lines.get(index).substring(4);
            String newPath = lines.get(index + 1).substring(4);
            index += 2;

            if ("/dev/null".equals(oldPath)) {
                throw new IllegalArgumentException("Whole-file additions are unsupported");
            }
            if ("/dev/null".equals(newPath)) {
                throw new IllegalArgumentException("Whole-file deletion is forbidden");
            }

            String path = pathFromNewHeader(newPath);
            if (proposedOutputHashes.containsKey(path)) {
                throw new IllegalArgumentException("Duplicate patch target: " + path);
            }
            List<String> filePatch = new ArrayList<>();
            while (index < lines.size() && !lines.get(index).startsWith("--- ")) {
                filePatch.add(lines.get(index++));
            }
            index--;
            if (filePatch.isEmpty()) {
                throw new IllegalArgumentException("Missing patch body for " + path);
            }

            String originalSource = generatedSources.get(path);
            if (originalSource == null) {
                throw new IllegalArgumentException("Patch targets undeclared source: " + path);
            }
            String proposedSource = applyPatch(originalSource, filePatch, path);
            proposedOutputHashes.put(path, PolishContracts.sha256(proposedSource));
        }
        if (proposedOutputHashes.isEmpty()) {
            throw new IllegalArgumentException("No patch operations found");
        }
        return proposedOutputHashes;
    }

    private String applyPatch(String original, List<String> filePatch, String path) {
        List<String> sourceLines = List.of(original.split("\\n", -1));
        List<String> applied = new ArrayList<>();
        int sourceIndex = 0;
        boolean sawHunk = false;

        for (int index = 0; index < filePatch.size(); index++) {
            String line = filePatch.get(index);
            if (line.isEmpty() || line.startsWith("\\ ")) {
                continue;
            }
            if (!line.startsWith("@@ ")) {
                throw new IllegalArgumentException("Invalid unified diff hunk for " + path);
            }
            Matcher matcher = HUNK_HEADER.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid unified diff hunk header for " + path);
            }
            sawHunk = true;

            int oldStart = Integer.parseInt(matcher.group(1));
            int oldCount = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));

            int targetOffset = oldStart - 1;
            if (targetOffset < sourceIndex) {
                throw new IllegalArgumentException("Out-of-order hunk for " + path);
            }
            while (sourceIndex < targetOffset) {
                applied.add(sourceLines.get(sourceIndex++));
            }

            int consumed = 0;
            index++;
            while (index < filePatch.size()) {
                String hunkLine = filePatch.get(index);
                if (hunkLine.startsWith("@@ ")) {
                    index--;
                    break;
                }
                if (hunkLine.startsWith("\\ ")) {
                    index++;
                    break;
                }
                if (hunkLine.isEmpty()) {
                    throw new IllegalArgumentException("Empty line in patch body for " + path);
                }
                char prefix = hunkLine.charAt(0);
                String content = hunkLine.substring(1);
                if (prefix == ' ') {
                    if (sourceIndex >= sourceLines.size()
                            || !sourceLines.get(sourceIndex).equals(content)) {
                        throw new IllegalArgumentException("Context mismatch for " + path);
                    }
                    sourceIndex++;
                    consumed++;
                    applied.add(content);
                } else if (prefix == '-') {
                    if (sourceIndex >= sourceLines.size()
                            || !sourceLines.get(sourceIndex).equals(content)) {
                        throw new IllegalArgumentException("Removed-content mismatch for " + path);
                    }
                    sourceIndex++;
                    consumed++;
                } else if (prefix == '+') {
                    applied.add(content);
                } else {
                    throw new IllegalArgumentException("Invalid patch body line for " + path);
                }
                index++;
            }

            if (consumed != oldCount) {
                throw new IllegalArgumentException("Hunk line-count mismatch for " + path);
            }
        }
        if (!sawHunk) {
            throw new IllegalArgumentException("No hunks found for " + path);
        }
        while (sourceIndex < sourceLines.size()) {
            applied.add(sourceLines.get(sourceIndex++));
        }
        return String.join("\n", applied);
    }

    private String pathFromNewHeader(String header) {
        if (!header.startsWith("b/")) {
            throw new IllegalArgumentException("Invalid new path");
        }
        return PolishContracts.javaPath(header.substring(2));
    }

    private GateCheckResult familyBoundary(PolishProposalRequest request, PolishFamilyPayload payload) {
        if (payload instanceof DomainNamingRefinement naming
                && (!request.nodeSelectors().containsKey(naming.nodeId())
                || !request.pathSelectors().keySet().containsAll(naming.referencePaths()))) {
            return GateCheckResult.failed("schema:unmapped-domain-boundary");
        }
        if (payload instanceof PortExtraction port
                && !request.pathSelectors().keySet().containsAll(port.callSites())) {
            return GateCheckResult.failed("schema:unmapped-port-boundary");
        }
        if (payload instanceof StrategyExtraction strategy
                && !request.nodeSelectors().containsKey(strategy.conditionalRegion())) {
            return GateCheckResult.failed("schema:unmapped-strategy-boundary");
        }
        return GateCheckResult.passed("schema:family-boundary-valid");
    }

    private Set<String> diffPaths(String diff) {
        TreeSet<String> oldPaths = new TreeSet<>();
        TreeSet<String> newPaths = new TreeSet<>();
        for (String line : lines(diff)) {
            if (line.startsWith("--- ")) {
                String path = line.substring(4);
                if (!"/dev/null".equals(path)) {
                    if (!path.startsWith("a/")) throw new IllegalArgumentException("Invalid old path");
                    oldPaths.add(PolishContracts.javaPath(path.substring(2)));
                }
            } else if (line.startsWith("+++ ")) {
                String path = line.substring(4);
                if ("/dev/null".equals(path) || !path.startsWith("b/")) {
                    throw new IllegalArgumentException("Whole-file deletion is forbidden");
                }
                newPaths.add(PolishContracts.javaPath(path.substring(2)));
            }
        }
        if (newPaths.isEmpty() || !newPaths.containsAll(oldPaths)) {
            throw new IllegalArgumentException("Diff headers are incomplete");
        }
        return Set.copyOf(newPaths);
    }

    private static List<String> lines(String diff) {
        return List.of(diff.substring(0, diff.length() - 1).split("\\n", -1));
    }
}
