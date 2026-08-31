package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.provider.cobol.guardrail.GateCheck;
import org.shark.renovatio.provider.cobol.guardrail.GateCheckResult;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailGateRunner;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailRunResult;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItemWriter;
import org.shark.renovatio.provider.cobol.guardrail.ProposalManifest;
import org.shark.renovatio.provider.cobol.guardrail.ReviewEligibilityRequest;
import org.shark.renovatio.provider.cobol.guardrail.ReviewEligibilityValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Produces inert review artifacts and intentionally exposes no apply or acceptance operation. */
public final class IdiomaticPolishService {

    private final ObjectMapper objectMapper;
    private final Path workspace;
    private final PolishCandidateGenerator generator;
    private final PolishValidationChecks checks;
    private final PolishContentIdentity identities;
    private final PolishWorkspaceSnapshot workspaceSnapshot = new PolishWorkspaceSnapshot();
    private final PolishCandidateValidator candidateValidator;
    private final PolishArtifactWriter artifactWriter;
    private final ManualActionItemWriter actionItemWriter;
    private final PolishActionItemFactory actionItemFactory = new PolishActionItemFactory();

    public IdiomaticPolishService(ObjectMapper objectMapper, Path workspace,
                                  PolishCandidateGenerator generator, PolishValidationChecks checks) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.workspace = Objects.requireNonNull(workspace, "workspace").toAbsolutePath();
        this.generator = Objects.requireNonNull(generator, "generator");
        this.checks = Objects.requireNonNull(checks, "checks");
        this.identities = new PolishContentIdentity(objectMapper);
        this.candidateValidator = new PolishCandidateValidator(objectMapper);
        this.artifactWriter = new PolishArtifactWriter(objectMapper);
        this.actionItemWriter = new ManualActionItemWriter(objectMapper);
    }

    public PolishProposalOutcome propose(PolishProposalRequest request,
                                         Collection<ManualActionItem> currentActionItems) throws IOException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(currentActionItems, "currentActionItems");
        String requestId = identities.requestId(request);
        String originalTreeHash = workspaceSnapshot.hash(workspace, request);
        AdmissionFailure admission = admissionFailure(request.evidence());
        if (admission != null) {
            return failure(PolishDisposition.INELIGIBLE, request, requestId, null,
                    admission.gate(), admission.diagnostic(), admission.executed(),
                    originalTreeHash, workspaceSnapshot.hash(workspace, request), currentActionItems);
        }

        String originalHash = identities.sourcesHash(request);
        final PolishCandidate candidate;
        try {
            candidate = Objects.requireNonNull(generator.generate(request), "generator returned null");
        } catch (RuntimeException exception) {
            String retainedTreeHash = workspaceSnapshot.hash(workspace, request);
            String diagnostic = originalTreeHash.equals(retainedTreeHash)
                    ? "polish:generation-failed" : "review:source-mutated";
            return failure(PolishDisposition.FAILED, request, requestId, null,
                    GuardrailGate.REVIEW_ELIGIBILITY, diagnostic,
                    List.of(),
                    originalTreeHash, retainedTreeHash, currentActionItems);
        }

        String proposalId = identities.proposalId(request, candidate);
        PolishProposalManifest manifest = manifest(proposalId, request, candidate, originalTreeHash);
        GuardrailRunResult run = gateRunner(request, candidate, manifest).run();
        String retainedTreeHash = workspaceSnapshot.hash(workspace, request);
        if (!originalHash.equals(identities.sourcesHash(request))
                || !originalTreeHash.equals(retainedTreeHash)) {
            return failure(PolishDisposition.FAILED, request, proposalId, candidate,
                    GuardrailGate.REVIEW_ELIGIBILITY, "review:source-mutated",
                    List.of(GuardrailGate.values()), originalTreeHash, retainedTreeHash,
                    currentActionItems);
        }
        if (!run.eligible()) {
            return failure(PolishDisposition.FAILED, request, proposalId, candidate,
                    run.failedGate(), run.diagnosticReference(), run.executedGates(),
                    originalTreeHash, retainedTreeHash, currentActionItems);
        }

        Path artifactDirectory = artifactWriter.write(workspace, manifest, candidate.unifiedDiff());
        return new PolishProposalOutcome(PolishDisposition.ELIGIBLE_FOR_REVIEW, proposalId,
                artifactDirectory, null, run.diagnosticReference(), run.executedGates(),
                originalTreeHash, retainedTreeHash, List.copyOf(currentActionItems));
    }

    private GuardrailGateRunner gateRunner(PolishProposalRequest request, PolishCandidate candidate,
                                           PolishProposalManifest manifest) {
        EnumMap<GuardrailGate, org.shark.renovatio.provider.cobol.guardrail.GateCheck> gates =
                new EnumMap<>(GuardrailGate.class);
        gates.put(GuardrailGate.SCHEMA, safe(GuardrailGate.SCHEMA, () -> {
            GateCheckResult builtIn = candidateValidator.validate(request, candidate, manifest);
            return builtIn.passed() ? checks.schema().validate(request, candidate) : builtIn;
        }));
        gates.put(GuardrailGate.COMPILATION, safe(GuardrailGate.COMPILATION,
                () -> checks.compilation().validate(request, candidate)));
        gates.put(GuardrailGate.CHARACTERIZATION, safe(GuardrailGate.CHARACTERIZATION,
                () -> checks.characterization().validate(request, candidate)));
        gates.put(GuardrailGate.REVIEW_ELIGIBILITY, safe(GuardrailGate.REVIEW_ELIGIBILITY,
                () -> new ReviewEligibilityValidator().validate(
                new ReviewEligibilityRequest(
                        new ProposalManifest(candidate.changedPaths(),
                                request.evidence().generatedInputHashes(), candidate.outputHashes()),
                        candidate.changedPaths(), candidate.publicSignatureChanges(),
                        candidate.ownerApprovedSignatures(), candidate.provenance(),
                        candidate.byteReproducible()))));
        return new GuardrailGateRunner(gates);
    }

    private GateCheck safe(GuardrailGate gate, GateCheck check) {
        return () -> {
            try {
                return check.execute();
            } catch (RuntimeException exception) {
                return GateCheckResult.failed(gate.externalName() + ":execution-failed");
            }
        };
    }

    private PolishProposalManifest manifest(String proposalId, PolishProposalRequest request,
                                             PolishCandidate candidate, String generatedTreeHash) {
        return new PolishProposalManifest(PolishProposalManifest.SCHEMA_VERSION, proposalId,
                request.family(), "PROPOSED", PolishDisposition.ELIGIBLE_FOR_REVIEW,
                request.evidence().repositoryCommit(), request.evidence().baselineRef(),
                request.evidence().characterizationSelectors(),
                request.evidence().characterizationCommand(),
                request.evidence().javaVersion(), request.evidence().mavenVersion(),
                request.evidence().generatedInputHashes(), request.evidence().semanticInputHashes(),
                request.evidence().expectedBehaviorHashes(), request.pathSelectors(),
                request.nodeSelectors(), candidate.outputHashes(), generatedTreeHash,
                candidate.changedPaths(), PolishContracts.sha256(candidate.unifiedDiff()),
                candidate.provenance(), candidate.publicSignatureChanges(),
                List.of(GuardrailGate.values()), "all-gates-passed",
                objectMapper.valueToTree(candidate.familyPayload()));
    }

    private AdmissionFailure admissionFailure(PolishPrerequisiteEvidence evidence) {
        List<GuardrailGate> executed = new ArrayList<>();
        executed.add(GuardrailGate.SCHEMA);
        if (!evidence.schemaGreen()) {
            return new AdmissionFailure(GuardrailGate.SCHEMA, "prerequisite:schema-not-green", executed);
        }
        executed.add(GuardrailGate.COMPILATION);
        if (!evidence.compilationGreen()) {
            return new AdmissionFailure(
                    GuardrailGate.COMPILATION, "prerequisite:compilation-not-green", executed);
        }
        executed.add(GuardrailGate.CHARACTERIZATION);
        if (!evidence.characterizationGreen()) {
            return new AdmissionFailure(
                    GuardrailGate.CHARACTERIZATION, "prerequisite:characterization-not-green", executed);
        }
        executed.add(GuardrailGate.REVIEW_ELIGIBILITY);
        if (!evidence.transliterationStable() || evidence.unresolvedErrorItems()) {
            return new AdmissionFailure(
                    GuardrailGate.REVIEW_ELIGIBILITY, "prerequisite:transliteration-ineligible", executed);
        }
        return null;
    }

    private PolishProposalOutcome failure(PolishDisposition disposition,
                                          PolishProposalRequest request,
                                          String proposalId,
                                          PolishCandidate candidate,
                                          GuardrailGate gate,
                                          String diagnostic,
                                          List<GuardrailGate> executed,
                                          String originalTreeHash,
                                          String retainedTreeHash,
                                          Collection<ManualActionItem> currentActionItems) throws IOException {
        ManualActionItem item = actionItemFactory.create(request, proposalId, gate, diagnostic, candidate);
        TreeMap<String, ManualActionItem> items = new TreeMap<>();
        currentActionItems.forEach(current -> items.put(current.id(), current));
        items.put(item.id(), item);
        actionItemWriter.write(workspace.resolve(ManualActionItemWriter.DEFAULT_REPORT), items.values());
        return new PolishProposalOutcome(disposition, proposalId, null, gate, diagnostic,
                executed, originalTreeHash, retainedTreeHash, List.copyOf(items.values()));
    }

    private record AdmissionFailure(
            GuardrailGate gate, String diagnostic, List<GuardrailGate> executed) {
        private AdmissionFailure {
            executed = List.copyOf(executed);
        }
    }
}
