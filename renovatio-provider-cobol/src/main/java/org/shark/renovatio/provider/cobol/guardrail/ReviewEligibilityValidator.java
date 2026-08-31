package org.shark.renovatio.provider.cobol.guardrail;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Fails closed unless a proposal is bounded, reproducible, attributable, and API-safe. */
public final class ReviewEligibilityValidator {

    public GateCheckResult validate(ReviewEligibilityRequest request) {
        Objects.requireNonNull(request, "request");
        ProposalManifest manifest = Objects.requireNonNull(request.manifest(), "manifest");
        Set<String> changedPaths = normalize(request.changedPaths(), "changedPaths");
        Set<String> signatureChanges = Set.copyOf(
                Objects.requireNonNull(request.publicSignatureChanges(), "publicSignatureChanges"));
        Set<String> approvals = Set.copyOf(
                Objects.requireNonNull(request.ownerApprovedSignatures(), "ownerApprovedSignatures"));
        Map<String, String> provenance = Map.copyOf(Objects.requireNonNull(request.provenance(), "provenance"));

        if (!request.byteReproducible()) {
            return GateCheckResult.failed("review:not-byte-reproducible");
        }
        if (!manifest.declaredPaths().containsAll(changedPaths)) {
            return GateCheckResult.failed("review:undeclared-path");
        }
        if (!manifest.outputHashes().keySet().containsAll(changedPaths)) {
            return GateCheckResult.failed("review:missing-output-hash");
        }
        if (manifest.inputHashes().isEmpty() || provenance.isEmpty()
                || provenance.values().stream().anyMatch(value -> value == null || value.isBlank())) {
            return GateCheckResult.failed("review:missing-provenance");
        }
        if (!approvals.containsAll(signatureChanges)) {
            return GateCheckResult.failed("review:unapproved-public-signature");
        }
        return GateCheckResult.passed("review:eligible");
    }

    private static Set<String> normalize(Set<String> paths, String name) {
        return Objects.requireNonNull(paths, name).stream()
                .map(path -> Path.of(path).normalize().toString().replace('\\', '/'))
                .collect(Collectors.toUnmodifiableSet());
    }
}
