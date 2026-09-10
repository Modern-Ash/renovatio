# Residual semantic enrichment test report

> GitHub issue: [#126](https://github.com/Modern-Ash/renovatio/issues/126)
> Agora work: `ai-modernization/residual-semantic-enrichment`
> Tested implementation commit: `f4193c6200919594e1c1303ece6ac97822c7c30b`
> Environment: OpenJDK 17 (`/usr/lib/jvm/java-17-openjdk-amd64`), Maven, Linux
> Executed: 2026-08-30, America/Argentina/Buenos_Aires

## Gate results

| Gate | Exact command | Scope | Tests | Result |
| --- | --- | --- | ---: | --- |
| Focused | `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -pl renovatio-llm,renovatio-cobol-ir -am test` | parent, shared, COBOL runtime, COBOL IR, LLM runtime | 168 | PASS: 0 failures, 0 errors, 0 skipped |
| Full reactor | `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn test` | all 10 reactor modules | 285 | PASS: 0 failures, 0 errors, 0 skipped |

Both commands exited zero and ended with `BUILD SUCCESS`. The Maven compiler used `release 17`.
The full-reactor `McpIntegrationTest` class discovered zero externally hosted cases because no local
servers were present on ports 8080/8081; the MCP module still executed its 22 offline tests
successfully. No residual enrichment test performs a live provider request.

## Acceptance mapping

| Criterion | Primary deterministic evidence | Result |
| --- | --- | --- |
| `domain-language` | `DomainNamingPolicyTest`, `ResidualAnnotationAssemblerTest` | Legal normalized Java identifiers, keyword/collision rejection, protected-signature advisory behavior, typed provenance: PASS |
| `goto-plan` | `ControlFlowPlanGateTest`, `ResidualEnrichmentCoordinatorTest` | Irreducible-only routing; green baseline retention; missing/red schema, compilation, or characterization discards the plan and emits `LLM_CHARACTERIZATION_NOT_GREEN`: PASS |
| `human-confirmation` | `HumanAnnotationReviewServiceTest`, `ResidualAnnotationAssemblerTest` | `NEEDS_REVIEW` by default; only `project:owner` may accept/reject; immutable proposal identity/payload/provenance; only accepted proposals consumable: PASS |
| `manual-actions` | `ManualMigrationActionsTest`, `ControlFlowPlanGateTest` | Precise content-addressed actions, stable deduplication, required evidence, rejection of unsupported preservation claims: PASS |
| `residual-only` | `ResidualEnrichmentCoordinatorTest` | MOVE, COMPUTE, IF, EVALUATE, simple PERFORM, basic PIC and level-88 execute with zero residual-runtime calls: PASS |

## Guardrail observations

- The implementation is additive under `renovatio-llm`; OpenRewrite recipes have no provider call.
- Base `CobolIntermediateModel` instances are never mutated by residual enrichment.
- Deterministic results remain distinct from model proposals in every routing outcome.
- Control-flow proposals are discarded before sidecar assembly unless commit-bound prerequisite
  evidence is green.
- Data-intent proposals cannot become downstream-consumable without the assigned human spec owner.
- Manual work items carry node, construction, reason, semantic risk, human action, closure evidence,
  diagnostic, and Agora tool-run provenance.

## Commit binding

The two test gates ran directly against implementation commit
`f4193c6200919594e1c1303ece6ac97822c7c30b`. This report and subsequent Agora-only metadata are the
only intended descendant changes before verification evidence is registered; they do not alter
production or test code.
