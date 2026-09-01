# F0 Decision-Model Cartography — Implementation Plan

- **Work item:** `decision-engine/f0-decision-cartography`
- **GitHub issue:** #145 (Epic #152)
- **Specification:** `docs/specs/decision-model-cartography.md`
- **Nature of work:** Documentation and evidence spike; no production-code changes
- **Plan status:** Coverage confirmed by the Spec Owner on 2026-08-31

## Objective

Turn the clarified cartography into a reproducible, governed result. The work
will verify the evidence snapshot, the 38-point catalog, category coverage, the
two-service coupling map, and the mechanically derived seven-decision F1 cut.

## Reproducibility baseline

- Canonical characterization fixtures are pinned to the clean PR base revision
  `b430ba48a01ebe55e42b9714a5ccf5557e3981aa`.
- `JavaGenerationService.java` is inspected at SHA-256
  `8ec5359ece8a48cc0c8891f235c770a9a5ac7dddc6c79e024f581a32361890c3`.
- `MigrationPlanService.java` is inspected at SHA-256
  `2e44a17db423b8a70d576aeaa89475f1cfe3e24d057e04fb1ece991dcd4803be`.
- The 13 fixture directories and their expected-behavior/action-item files are
  the frequency corpus. Missing IR and generated-Java goldens remain explicit
  corpus limitations rather than inferred evidence.

## Execution steps

1. **Validate the snapshot.** Confirm the pinned revision, service hashes,
   fixture count, and fixture-side evidence described in the specification.
   Record any drift instead of silently changing the baseline.
2. **Verify the decision catalog.** Check that every catalog row has category,
   location, current option, alternatives, confidence, LLM recommendation,
   characterization flag, and frequency; confirm the catalog contains at least
   15 decisions.
3. **Verify category evidence.** Confirm that NUMERIC, CONTROL_FLOW,
   DATA_SHAPE, PERSISTENCE, NAMING, and ARCHITECTURE each cite at least one real
   fixture example, including the declared persistence corpus gap.
4. **Verify the coupling map.** Re-read both target services and their direct
   and transitive collaborators against the captured hashes. Check parameters,
   collaborators, configuration, filesystem and ambient state, static state,
   semantic IR, annotations, and migration-run state.
5. **Recompute the F1 cut.** Apply the strict rule independently: frequency at
   least 3 or structural applicability to all programs, confidence H, and
   characterization-verifiable. Confirm that it selects exactly decisions
   #1, #27, #28, #30, #33, #37, and #38, with no exceptions.
6. **Produce verification evidence.** Write a concise verification report with
   the commands/checks and results, run Agora consistency validation, and
   register the resulting evidence before satisfying the criteria.

## Acceptance-criterion traceability

| Criterion | Covered by | Required evidence |
|---|---|---|
| `catalog` | Steps 1–2 | Catalog schema check and counted decision rows |
| `categories` | Steps 1 and 3 | Six-category fixture citation check |
| `coupling-map` | Steps 1 and 4 | Service/collaborator inspection at recorded hashes |
| `f1-recommendation` | Steps 1 and 5 | Independent strict-rule recomputation |

All four criteria must reach the Agora `planned` stage through Spec Owner
confirmation before implementation begins.

## Deliverables

- Governed specification: `docs/specs/decision-model-cartography.md`
- This implementation plan
- Verification report: `docs/reports/decision-model-cartography-verification.md`
- Agora consistency report and criterion evidence records

## Verification and completion gates

- The baseline values reproduce or any drift is documented and resolved.
- Automated/document checks pass and `git diff --check` reports no formatting
  errors in the scoped artifacts.
- `agora work verify-consistency` succeeds after final artifact registration.
- Each acceptance criterion is supported by evidence, verified, and accepted
  through its assigned Agora role.
- The spike changes no production source or fixture behavior.

## Non-goals

- Implementing the F1 decision engine or any of its seven selected decisions.
- Modifying characterization fixtures or production services.
- Introducing a new semantic IR, an LLM decision producer, or a transport for
  LLM suggestions.
- Expanding the catalog beyond what is necessary to correct verified errors.
