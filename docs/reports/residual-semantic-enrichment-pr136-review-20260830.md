# Residual semantic enrichment PR #136 review verification

> GitHub issue: [#126](https://github.com/Modern-Ash/renovatio/issues/126)
> Pull request: [#136](https://github.com/Modern-Ash/renovatio/pull/136)
> Agora work: `ai-modernization/residual-semantic-enrichment`, revision 2
> Tested commit: `f1d143fdfd7694e4e459d7d27bd2048eaee89fa7`
> Environment: OpenJDK 17 (`/usr/lib/jvm/java-17-openjdk-amd64`), Maven, Linux
> Executed: 2026-08-30, America/Argentina/Buenos_Aires

## Gate results

| Gate | Exact command | Tests | Result |
| --- | --- | ---: | --- |
| Focused | `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl renovatio-llm,renovatio-cobol-ir -am test` | 175 | PASS: 0 failures, 0 errors, 0 skipped |
| Full reactor | `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn test` | 292 | PASS: 0 failures, 0 errors, 0 skipped |
| Agora integrity | `agora validate` | 78 governed documents | PASS: `ok: true` |

## Review finding coverage

| Finding | Correction | Regression evidence |
| --- | --- | --- |
| `production-routing-not-wired` | `LlmEnrichmentCli` invokes `ResidualEnrichmentCoordinator`; the selected route owns the prompt ID and deterministic constructions return before cache/provider setup. | `LlmEnrichmentCliTest.deterministicConstructionBypassesPromptCacheAndProvider`, `callerPromptMustMatchTheDeterministicallySelectedResidualRoute` |
| `annotation-identity-conflict` | Existing identities deduplicate equal output hashes and reject conflicting hashes without appending. | `ResidualAnnotationAssemblerTest.repeatedIdentityIsIdempotentAndConflictingOutputIsRejected` |
| `control-flow-baseline-unbound` | `ResidualAnnotationContext` carries the expected characterization baseline and the gate requires an exact match. | `ControlFlowPlanGateTest.evidenceFromAnotherBaselineIsRejected` |
| `domain-review-transition-missing` | Domain naming proposals require the spec owner, enter `NEEDS_REVIEW`, and support accept/reject transitions. | `HumanAnnotationReviewServiceTest.specOwnerCanReviewDomainNamingProposal` |
| `aggregate-evidence-uri-stale` | Revision 2 starts a fresh aggregate register and records this report under its own immutable URI; revision 1 remains an immutable historical snapshot. | `agora validate` returns `ok: true` |

Both test commands ended with `BUILD SUCCESS`. No production or test source changed after the tested
commit; subsequent changes are limited to this report and Agora revision-2 metadata.
