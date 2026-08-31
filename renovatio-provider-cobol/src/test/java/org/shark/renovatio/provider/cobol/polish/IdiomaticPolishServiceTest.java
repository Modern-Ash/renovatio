package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.provider.cobol.guardrail.GateCheckResult;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailGate;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionReviewStatus;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionSeverity;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItemWriter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IdiomaticPolishServiceTest {

    private static final String HASH = "a".repeat(64);
    private static final String SOURCE = "class Customer { String customerCode; }\n";
    private static final String PROPOSED_SOURCE = "class Customer { String accountCode; }\n";
    private static final String OUTPUT_HASH = PolishContracts.sha256(PROPOSED_SOURCE);
    private static final String PATH = "Customer.java";

    @TempDir
    Path workspace;

    @Test
    void redPrerequisiteProducesNoGeneratorCallAndReplacesStaleReport() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        PolishProposalRequest request = request(evidence(
                true, false, true, true, false));
        seedGeneratedSources(request, workspace);
        ManualActionItem unrelated = action("mai-000000000000000000000001", "current");
        ManualActionItem stale = action("mai-000000000000000000000002", "stale");
        new ManualActionItemWriter(new ObjectMapper()).write(
                workspace.resolve(ManualActionItemWriter.DEFAULT_REPORT), List.of(stale));

        IdiomaticPolishService service = service(ignored -> {
            calls.incrementAndGet();
            return candidate();
        }, passingChecks());

        PolishProposalOutcome outcome = service.propose(request, List.of(unrelated));

        assertThat(outcome.disposition()).isEqualTo(PolishDisposition.INELIGIBLE);
        assertThat(outcome.failedGate()).isEqualTo(GuardrailGate.COMPILATION);
        assertThat(calls).hasValue(0);
        assertThat(outcome.actionItems()).hasSize(2).extracting(ManualActionItem::reason)
                .contains("current")
                .noneMatch("stale"::equals);
        assertThat(Files.exists(workspace.resolve("build/reports/renovatio/idiomatic-polish")))
                .isFalse();
        JsonNode report = new ObjectMapper().readTree(
                workspace.resolve(ManualActionItemWriter.DEFAULT_REPORT).toFile());
        assertThat(report.path("items")).hasSize(2);
        assertThat(report.toString()).doesNotContain("stale");
    }

    @Test
    void everyRedPrerequisiteSkipsCandidateGeneration() throws Exception {
        List<PolishPrerequisiteEvidence> redEvidence = List.of(
                evidence(false, true, true, true, false),
                evidence(true, false, true, true, false),
                evidence(true, true, false, true, false),
                evidence(true, true, true, false, false),
                evidence(true, true, true, true, true));
        AtomicInteger calls = new AtomicInteger();

        for (int index = 0; index < redEvidence.size(); index++) {
            Path run = workspace.resolve("red-" + index);
            PolishProposalRequest request = request(redEvidence.get(index));
            seedGeneratedSources(request, run);
            IdiomaticPolishService service = new IdiomaticPolishService(new ObjectMapper(), run, ignored -> {
                calls.incrementAndGet();
                return candidate();
            }, passingChecks());
            PolishProposalOutcome outcome = service.propose(request, List.of());

            assertThat(outcome.disposition()).isEqualTo(PolishDisposition.INELIGIBLE);
            assertThat(outcome.artifactDirectory()).isNull();
        }
        assertThat(calls).hasValue(0);
    }

    @Test
    void everyCandidateGateFailureStopsLaterGatesAndDiscardsPatch() throws Exception {
        List<GateFailure> failures = List.of(
                new GateFailure(GuardrailGate.SCHEMA, "schema:forced",
                checks("schema:forced", null, null), candidate()),
                new GateFailure(GuardrailGate.COMPILATION, "compilation:forced",
                        checks(null, "compilation:forced", null), candidate()),
                new GateFailure(GuardrailGate.CHARACTERIZATION, "characterization:forced",
                        checks(null, null, "characterization:forced"), candidate()),
                new GateFailure(GuardrailGate.REVIEW_ELIGIBILITY,
                        "review:unapproved-public-signature", passingChecks(),
                        candidate(Set.of("Customer#rename"), Set.of(), true)));

        for (int index = 0; index < failures.size(); index++) {
            GateFailure failure = failures.get(index);
            Path run = workspace.resolve("gate-" + index);
            PolishProposalRequest request = request(evidence(true, true, true, true, false));
            seedGeneratedSources(request, run);
            IdiomaticPolishService service = new IdiomaticPolishService(
                    new ObjectMapper(), run, ignored -> failure.candidate(), failure.checks());
            PolishProposalOutcome outcome = service.propose(request, List.of());

            assertThat(outcome.disposition()).isEqualTo(PolishDisposition.FAILED);
            assertThat(outcome.failedGate()).isEqualTo(failure.gate());
            assertThat(outcome.diagnosticReference()).isEqualTo(failure.diagnostic());
            assertThat(outcome.executedGates()).containsExactly(
                    java.util.Arrays.copyOfRange(GuardrailGate.values(), 0, failure.gate().ordinal() + 1));
            assertThat(outcome.artifactDirectory()).isNull();
            assertThat(Files.exists(run.resolve("build/reports/renovatio/idiomatic-polish")))
                    .isFalse();
        }
    }

    @Test
    void undeclaredDiffPathFailsBuiltInSchemaBeforeInjectedValidation() throws Exception {
        AtomicInteger injectedSchemaCalls = new AtomicInteger();
        PolishValidationChecks checks = new PolishValidationChecks(
                (request, candidate) -> {
                    injectedSchemaCalls.incrementAndGet();
                    return GateCheckResult.passed("schema:green");
                },
                (request, candidate) -> GateCheckResult.passed("compilation:green"),
                (request, candidate) -> GateCheckResult.passed("characterization:green"));
        PolishCandidate candidate = new PolishCandidate(
                "--- a/Other.java\n+++ b/Other.java\n@@ -1 +1 @@\n-class Other {}\n+class Other { }\n",
                Set.of("Other.java"), Map.of("Other.java", OUTPUT_HASH), Set.of(), Set.of(),
                provenance(PolishProposalFamily.DOMAIN_NAMING_REFINEMENT),
                new DomainNamingRefinement("node-1", "customerCode", "accountCode",
                        Set.of(PATH), true, false, false), true);
        seedGeneratedSources(request(evidence(true, true, true, true, false)), workspace);

        PolishProposalOutcome outcome = service(ignored -> candidate, checks).propose(
                request(evidence(true, true, true, true, false)), List.of());

        assertThat(outcome.failedGate()).isEqualTo(GuardrailGate.SCHEMA);
        assertThat(outcome.diagnosticReference()).isEqualTo("schema:undeclared-changed-path");
        assertThat(injectedSchemaCalls).hasValue(0);
    }

    @Test
    void generationFailureRetainsNoPatchAndWritesAStableActionItem() throws Exception {
        IdiomaticPolishService service = service(ignored -> {
            throw new IllegalStateException("provider unavailable");
        }, passingChecks());
        PolishProposalRequest request = request(evidence(true, true, true, true, false));
        seedGeneratedSources(request, workspace);

        PolishProposalOutcome first = service.propose(
                request, List.of());
        PolishProposalOutcome second = service.propose(
                request, List.of());

        assertThat(first.disposition()).isEqualTo(PolishDisposition.FAILED);
        assertThat(first.diagnosticReference()).isEqualTo("polish:generation-failed");
        assertThat(first.executedGates()).isEmpty();
        assertThat(first.artifactDirectory()).isNull();
        assertThat(first.actionItems()).extracting(ManualActionItem::id)
                .containsExactlyElementsOf(second.actionItems().stream().map(ManualActionItem::id).toList());
    }

    @Test
    void candidateWithBadOutputHashesFailsBuiltInSchemaValidation() throws Exception {
        PolishCandidate candidate = candidate(
                Set.of(), Set.of(), true, Map.of(PATH, "b".repeat(64)),
                Set.of(PATH));
        PolishProposalRequest request = request(evidence(true, true, true, true, false));
        seedGeneratedSources(request, workspace);

        PolishProposalOutcome outcome = service(ignored -> candidate, passingChecks()).propose(
                request, List.of());

        assertThat(outcome.disposition()).isEqualTo(PolishDisposition.FAILED);
        assertThat(outcome.failedGate()).isEqualTo(GuardrailGate.SCHEMA);
        assertThat(outcome.diagnosticReference()).isEqualTo("schema:output-hash-mismatch");
        assertThat(outcome.artifactDirectory()).isNull();
    }

    @Test
    void generationMutationToGeneratedSourceIsIsolatedAndClassifiedAsSourceMutation() throws Exception {
        PolishProposalRequest request = request(evidence(
                true, true, true, true, false));
        Map<String, String> original = request.generatedSources();
        seedGeneratedSources(request, workspace);
        Path generated = workspace.resolve("generated").resolve(PATH);

        IdiomaticPolishService service = service(ignored -> {
            try {
                Files.writeString(generated, "class Customer { String badCode; }\n");
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
            return candidate();
        }, passingChecks());

        PolishProposalOutcome outcome = service.propose(request, List.of());

        assertThat(outcome.disposition()).isEqualTo(PolishDisposition.FAILED);
        assertThat(outcome.failedGate()).isEqualTo(GuardrailGate.REVIEW_ELIGIBILITY);
        assertThat(outcome.diagnosticReference()).isEqualTo("review:source-mutated");
        assertThat(outcome.executedGates()).isEmpty();
        assertThat(Files.readString(generated)).isEqualTo(original.get(PATH));
    }

    @Test
    void successfulProposalReplacesStaleManualActionReport() throws Exception {
        ManualActionItem stale = action("mai-000000000000000000000003", "stale");
        new ManualActionItemWriter(new ObjectMapper()).write(
                workspace.resolve(ManualActionItemWriter.DEFAULT_REPORT),
                List.of(stale));

        PolishProposalRequest request = request(evidence(
                true, true, true, true, false));
        Path generated = workspace.resolve("generated").resolve(PATH);
        Files.createDirectories(generated.getParent());
        Files.writeString(generated, SOURCE);

        PolishProposalOutcome outcome = service(ignored -> candidate(), passingChecks()).propose(
                request, List.of());

        assertThat(outcome.disposition()).isEqualTo(PolishDisposition.ELIGIBLE_FOR_REVIEW);
        JsonNode report = new ObjectMapper().readTree(
                workspace.resolve(ManualActionItemWriter.DEFAULT_REPORT).toFile());
        assertThat(report.path("items")).isEmpty();
    }

    @Test
    void successfulProposalEmitsOnlyStableReviewArtifactsAndNeverChangesInputs() throws Exception {
        PolishProposalRequest request = request(evidence(
                true, true, true, true, false));
        Map<String, String> originalSources = request.generatedSources();
        Path generated = workspace.resolve("generated").resolve(PATH);
        Files.createDirectories(generated.getParent());
        Files.writeString(generated, originalSources.get(PATH));
        byte[] originalWorkspaceSource = Files.readAllBytes(generated);
        IdiomaticPolishService service = service(ignored -> candidate(), passingChecks());

        PolishProposalOutcome first = service.propose(request, List.of());
        byte[] firstPatch = Files.readAllBytes(first.artifactDirectory().resolve("proposal.patch"));
        byte[] firstManifest = Files.readAllBytes(first.artifactDirectory().resolve("manifest.json"));
        PolishProposalOutcome second = service.propose(request, List.of());

        assertThat(first.disposition()).isEqualTo(PolishDisposition.ELIGIBLE_FOR_REVIEW);
        assertThat(first.failedGate()).isNull();
        assertThat(first.executedGates()).containsExactly(GuardrailGate.values());
        assertThat(first.artifactDirectory()).isEqualTo(second.artifactDirectory());
        assertThat(Files.readAllBytes(second.artifactDirectory().resolve("proposal.patch")))
                .isEqualTo(firstPatch);
        assertThat(Files.readAllBytes(second.artifactDirectory().resolve("manifest.json")))
                .isEqualTo(firstManifest);
        assertThat(Files.list(first.artifactDirectory()).map(path -> path.getFileName().toString()))
                .containsExactlyInAnyOrder("proposal.patch", "manifest.json");
        assertThat(request.generatedSources()).isEqualTo(originalSources);
        assertThat(Files.readAllBytes(generated)).isEqualTo(originalWorkspaceSource);
        assertThat(first.originalTreeHash()).isEqualTo(first.retainedTreeHash());
        JsonNode manifest = new ObjectMapper().readTree(firstManifest);
        assertThat(manifest.path("reviewState").asText()).isEqualTo("PROPOSED");
        assertThat(manifest.path("disposition").asText()).isEqualTo("ELIGIBLE_FOR_REVIEW");
    }

    @Test
    void eachClosedFamilyProducesOnlyAProposedReviewArtifact() throws Exception {
        List<PolishFamilyPayload> families = List.of(
                new DomainNamingRefinement("node-1", "customerCode", "accountCode",
                        Set.of(PATH), true, false, false),
                new PortExtraction("httpClient", "CustomerPort", Set.of(PATH), true, false),
                new StrategyExtraction("node-1", Set.of("ACTIVE", "CLOSED"), true, true),
                new FlagCollapse(Set.of("active", "closed"), Set.of("10", "01"),
                        Map.of("10", "ACTIVE", "01", "CLOSED"), true, true));

        for (int index = 0; index < families.size(); index++) {
            PolishFamilyPayload payload = families.get(index);
            Path run = workspace.resolve("family-" + index);
            PolishCandidate candidate = candidate(payload, Set.of(), Set.of(), true);
            IdiomaticPolishService service = new IdiomaticPolishService(
                    new ObjectMapper(), run, ignored -> candidate, passingChecks());
            PolishProposalRequest request = request(payload, evidence(
                    true, true, true, true, false));
            seedGeneratedSources(request, run);

            PolishProposalOutcome outcome = service.propose(
                    request,
                    List.of());

            assertThat(outcome.disposition()).isEqualTo(PolishDisposition.ELIGIBLE_FOR_REVIEW);
            JsonNode manifest = new ObjectMapper().readTree(
                    outcome.artifactDirectory().resolve("manifest.json").toFile());
            assertThat(manifest.path("family").asText()).isEqualTo(payload.family().name());
            assertThat(manifest.path("reviewState").asText()).isEqualTo("PROPOSED");
        }
    }

    @Test
    void publicServiceHasNoApplyOrAcceptOperation() {
        assertThat(List.of(IdiomaticPolishService.class.getDeclaredMethods()))
                .extracting(method -> method.getName().toLowerCase())
                .contains("propose")
                .noneMatch(name -> Set.of("apply", "accept", "commit", "merge").contains(name));
    }

    private IdiomaticPolishService service(PolishCandidateGenerator generator,
                                           PolishValidationChecks checks) {
        return new IdiomaticPolishService(new ObjectMapper(), workspace, generator, checks);
    }

    private PolishPrerequisiteEvidence evidence(boolean schema, boolean compilation,
                                               boolean characterization, boolean stable,
                                               boolean unresolvedErrors) {
        WorkspaceCommitState commitState = workspaceCommitState();
        String repositoryCommit = commitState == null ? "abc123" : commitState.repositoryCommit();
        String baselineRef = commitState == null ? "baseline-1" : commitState.baselineRef();
        return new PolishPrerequisiteEvidence(
                repositoryCommit, baselineRef, List.of("move-numeric"),
                "mvn -B -pl renovatio-provider-cobol -am test", "17", "3.9.12",
                Map.of("Customer.java", PolishContracts.sha256(SOURCE)),
                Map.of("node-1", PolishContracts.canonicalJsonHash(PolishContractsTest.projection())),
                Map.of("move-numeric", HASH),
                schema, compilation, characterization, stable, unresolvedErrors);
    }

    private PolishProposalRequest request(PolishPrerequisiteEvidence evidence) {
        DomainNamingRefinement payload = new DomainNamingRefinement(
                "node-1", "customerCode", "accountCode", Set.of(PATH), true, false, false);
        return request(payload, evidence);
    }

    private PolishProposalRequest request(PolishFamilyPayload payload,
                                           PolishPrerequisiteEvidence evidence) {
        return new PolishProposalRequest(payload.family(), "input.cob", "SAMPLE", HASH,
                "generated", Map.of(PATH, SOURCE),
                Map.of("node-1", PolishContractsTest.projection()),
                Map.of(PATH, "move-numeric"), Map.of("node-1", "move-numeric"), evidence);
    }

    private PolishCandidate candidate() {
        return candidate(Set.of(), Set.of(), true);
    }

    private PolishCandidate candidate(Set<String> signatures, Set<String> approvals,
                                      boolean reproducible) {
        return candidate(new DomainNamingRefinement(
                "node-1", "customerCode", "accountCode", Set.of(PATH), true, false, false),
                signatures, approvals, reproducible);
    }

    private PolishCandidate candidate(PolishFamilyPayload payload, Set<String> signatures,
                                      Set<String> approvals, boolean reproducible) {
        return new PolishCandidate(
                "--- a/Customer.java\n+++ b/Customer.java\n@@ -1 +1 @@\n"
                        + "-" + SOURCE
                        + "+" + PROPOSED_SOURCE,
                Set.of(PATH), Map.of(PATH, OUTPUT_HASH), signatures, approvals,
                provenance(payload.family()),
                payload, reproducible);
    }

    private PolishCandidate candidate(Set<String> signatures, Set<String> approvals,
                                      boolean reproducible, Map<String, String> outputHashes,
                                      Set<String> changedPaths) {
        return new PolishCandidate(
                "--- a/Customer.java\n+++ b/Customer.java\n@@ -1 +1 @@\n"
                        + "-" + SOURCE
                        + "+" + PROPOSED_SOURCE,
                changedPaths, outputHashes, signatures, approvals,
                provenance(PolishProposalFamily.DOMAIN_NAMING_REFINEMENT),
                new DomainNamingRefinement("node-1", "customerCode", "accountCode",
                        Set.of(PATH), true, false, false), reproducible);
    }

    private Map<String, String> provenance(PolishProposalFamily family) {
        return Map.ofEntries(
                Map.entry("promptId", family.promptId()),
                Map.entry("promptVersion", "1"),
                Map.entry("promptHash", HASH),
                Map.entry("outputSchemaHash", HASH),
                Map.entry("validators", "strict-json,java17"),
                Map.entry("cacheKey", HASH),
                Map.entry("cacheHash", HASH),
                Map.entry("providerId", "offline-fake"),
                Map.entry("modelId", "offline-fake"),
                Map.entry("agoraToolRun", "tool-test"),
                Map.entry("resultDisposition", "MODEL_SUCCESS"));
    }

    private PolishValidationChecks checks(String schemaFailure, String compilationFailure,
                                           String characterizationFailure) {
        return new PolishValidationChecks(
                (request, candidate) -> result(schemaFailure, "schema:green"),
                (request, candidate) -> result(compilationFailure, "compilation:green"),
                (request, candidate) -> result(characterizationFailure, "characterization:green"));
    }

    private GateCheckResult result(String failure, String success) {
        return failure == null ? GateCheckResult.passed(success) : GateCheckResult.failed(failure);
    }

    private PolishValidationChecks passingChecks() {
        return new PolishValidationChecks(
                (request, candidate) -> GateCheckResult.passed("schema:green"),
                (request, candidate) -> GateCheckResult.passed("compilation:green"),
                (request, candidate) -> GateCheckResult.passed("characterization:green"));
    }

    private ManualActionItem action(String id, String reason) {
        return new ManualActionItem(id, "input.cob", "SAMPLE", null, null, null, null,
                "node-1", HASH, "POLISH", reason, GuardrailGate.REVIEW_ELIGIBILITY,
                "TEST", "Deterministic Java retained", "Review manually", "Tests pass",
                ManualActionSeverity.WARNING, ManualActionReviewStatus.PENDING,
                null, null, null, null, null, null);
    }

    private record GateFailure(GuardrailGate gate, String diagnostic,
                               PolishValidationChecks checks, PolishCandidate candidate) { }

    private void seedGeneratedSources(PolishProposalRequest request, Path targetWorkspace) {
        request.generatedSources().forEach((path, content) -> {
            try {
                Path output = targetWorkspace.resolve(request.generatedRoot()).resolve(path);
                Files.createDirectories(output.getParent());
                Files.writeString(output, content, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE);
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        });
    }

    private WorkspaceCommitState workspaceCommitState() {
        try (Repository repository = new FileRepositoryBuilder()
                .setWorkTree(workspace.toFile())
                .readEnvironment()
                .findGitDir()
                .build()) {
            if (repository.getDirectory() == null) {
                return null;
            }
            ObjectId head = repository.resolve(Constants.HEAD);
            if (head == null) {
                return null;
            }
            return new WorkspaceCommitState(head.name(), repository.getFullBranch());
        } catch (Exception error) {
            return null;
        }
    }

    private record WorkspaceCommitState(String repositoryCommit, String baselineRef) { }
}
