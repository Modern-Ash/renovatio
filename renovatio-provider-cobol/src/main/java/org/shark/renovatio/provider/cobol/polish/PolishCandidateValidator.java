package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.shark.renovatio.provider.cobol.guardrail.GateCheckResult;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailSchemaCatalog;

import java.util.Set;
import java.util.TreeSet;

final class PolishCandidateValidator {

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
        for (String line : diff.split("\n", -1)) {
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
}
