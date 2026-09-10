# Issue 228 corrective implementation plan

## Delivery sequence

1. Replace the pipeline facade's placeholder stage messages with production parser, intermediate
   model, semantic projection, domain projection, architecture, manifest, generation, build, and
   equivalence results. A reported successful stage must be backed by an executed capability.
2. Bind each request to a deterministic source snapshot. Reject execution when the current source
   differs from the snapshot and expose a stable blocking action item.
3. Validate determinism and idempotency from relative generated paths and file bytes in independent
   output trees. Add focused tests that prove content drift is detected.
4. Convert unsupported semantic evidence and generation manual actions into stable pipeline action
   items. A successful result must not conceal unresolved blocking gaps.
5. Route a complete reference migration through the application-facing service and MCP adapter,
   using the same pipeline orchestrator as direct execution.
6. Update the committed runbook for Java 21, exact wrapper commands, clean-checkout execution, and
   measured elapsed time. Register fixture, equivalence, and test evidence as Agora artifacts.
7. Run focused tests, the COBOL provider reactor, the characterization guardrail, and GitHub checks.
   Resolve each structured review finding only when the corresponding evidence exists.

## Compatibility and architecture constraints

- Keep the source provider independent of the Java target provider and transport layer.
- Keep existing fixture layout and expected-output contracts unless a production-backed stage
  demonstrates that a fixture expectation is incorrect.
- Preserve compatibility constructors where callers outside this module may use the public records.
- Keep the reference run offline and deterministic; no LLM or network dependency is permitted.

## Verification matrix

| Criterion | Primary evidence |
| --- | --- |
| fixtures | fixture inventory and existence tests |
| end-to-end | pipeline tests for all three fixtures and stage execution assertions |
| determinism | independent generated-tree byte comparison tests |
| idempotency | repeated execution and stale snapshot rejection tests |
| semantic-gaps | stable action-item tests for unsupported semantics |
| equivalence | missing/unexpected/content-difference tests and three fixture reports |
| surface-proof | direct/service/MCP executions with identical output snapshots |
| runbook | clean-checkout Java 21 command log and elapsed time |

## Completion rule

Implementation can enter verification only after every critical or high audit finding has a code or
documentation change and focused test coverage. Completion additionally requires all six artifacts,
criterion acceptance by the spec owner, recorded test evidence tied to a commit, and successful
checks on the published corrective branch.
