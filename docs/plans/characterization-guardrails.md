# Implementation Plan: Characterization Harness and Guardrails

> GitHub issue: [#122](https://github.com/Modern-Ash/renovatio/issues/122)  
> Agora work: `ai-modernization/characterization-guardrails`  
> Specification: `docs/specs/characterization-guardrails.md`

## 1. Outcome

Deliver an executable, offline-capable characterization harness that proves deterministic COBOL
translation behavior, enforces the ordered admission gates, and produces a schema-valid manual
action report whenever a proposal is rejected. This plan does not add an LLM provider or implement
the semantic expansion tracked by later issues.

## 2. Delivery sequence

### Step 1 — Pin the test environment and schemas

- Pin Java 17, Maven 3.9.12, and an immutable digest for the selected Java 17 Maven container in the
  CI workflow.
- Add `cobol-ir.v1.schema.json` and `manual-action-item.v1.schema.json` in versioned schema resource
  directories owned by the COBOL IR and provider modules.
- Add a schema resolver that rejects unknown versions and fails closed.
- Reserve `annotated-cobol-model.v1.schema.json` as a dependency supplied by issue #124; annotated
  output remains ineligible until it exists.

### Step 2 — Build the fixture loader and canonical comparison layer

- Add a JUnit 5 fixture harness under `renovatio-provider-cobol/src/test/java`.
- Load fixtures from
  `renovatio-provider-cobol/src/test/resources/characterization/<fixture-id>/`.
- Canonicalize JSON deterministically and normalize generated Java only through the repository's
  configured formatter; do not erase semantic differences.
- Compare IR, Java, observable behavior, diagnostics, and action items according to the supported
  versus residual file contract in the specification.
- Fail when required expectations are missing or undeclared output is produced.

### Step 3 — Commit the twelve-fixture baseline

Create the exact fixture ids declared by the specification:

1. `move-numeric`
2. `move-alphanumeric-boundaries`
3. `compute-decimal-sign`
4. `if-nested`
5. `evaluate-level-88`
6. `perform-simple-nested`
7. `goto-reducible`
8. `goto-irreducible`
9. `redefines-overlap`
10. `odo-valid-boundary`
11. `odo-invalid-count`
12. `unsupported-construct`

Each supported fixture must prove byte-stable IR/Java and observable behavior. Each residual fixture
must prove that speculative Java is absent and the expected manual action is present.

### Step 4 — Implement deterministic fallback reporting

- Introduce a provider-neutral manual action-item model matching
  `manual-action-item.v1.schema.json`.
- Add a deterministic writer for `build/reports/renovatio/manual-action-items.json`.
- Use stable ordering and content-derived identifiers.
- Redact credentials, provider metadata not listed by the schema, and unnecessary source content.
- Add focused unit tests for safe transliteration, fail-closed refusal, stable ids, and redaction.

### Step 5 — Enforce the ordered gate runner

- Implement one orchestration boundary that executes, in order: schema validation, Java
  compilation, characterization tests, then review-eligibility validation.
- Stop at the first failure and record its gate and diagnostic in the action report.
- Add negative tests that force one failure at each gate and prove later gates are not executed.
- Ensure the runner has no provider client, prompt, credential, or network dependency.

### Step 6 — Validate review eligibility

- Require a proposal manifest listing every path that may change.
- Reject undeclared file changes and unexpected public-signature changes.
- Require provenance and content hashes for proposal inputs and outputs.
- Allow a public API change only when its exact fully qualified signature and compatibility impact
  are present in the reviewed plan evidence.

### Step 7 — Add the offline CI lane

- Pre-populate the Maven dependency cache in a network-enabled setup step that has no provider
  credentials.
- Run the governed test step inside the pinned container with `--network=none` and Maven `-o`.
- Execute negative DNS and outbound-connectivity probes and require both to fail.
- Assert that known LLM credential variables are absent.
- Upload Surefire reports, generated diffs, connectivity evidence, and manual action reports.

### Step 8 — Produce governed evidence

- Register the test plan before running the final suite.
- Run the module build and characterization commands from the specification.
- Register a successful test report containing the fixture matrix, forced gate failures,
  reproducibility check, and offline-lane result.
- Attach the CI run and immutable container digest as evidence references.

## 3. Acceptance-criterion coverage

| Criterion | Planned coverage |
| --- | --- |
| `golden-fixtures` | Steps 2 and 3 create the strict loader and twelve committed fixtures. |
| `gate-order` | Steps 5 and 6 enforce order, fail-fast behavior, bounded diffs, and API checks. |
| `safe-fallback` | Step 4 implements deterministic transliteration/refusal and action-item reporting. |
| `offline-ci` | Steps 1 and 7 pin the environment and prove network and credential isolation. |

## 4. Verification commands

```bash
mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am -DskipTests package
mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test
```

The CI variant executes the same test selection with Maven offline inside the network-disabled
container. Re-running the supported fixtures twice must produce identical SHA-256 hashes.

## 5. Risks and controls

- **Parser coverage gap:** residual fixtures may not produce safe IR; the harness requires an action
  item and forbids speculative Java instead of weakening the expectation.
- **False determinism from broad normalization:** canonicalization is limited to JSON key order and
  the configured Java formatter; behavioral fields cannot be ignored.
- **Offline lane accidentally reaching the network:** container network isolation and explicit
  negative probes are both required.
- **Scope creep into semantic implementation:** fixture expectations may expose gaps, but semantic
  additions remain in their dependent issues unless required solely to make the harness executable.

## 6. Exit condition for planning

Before transition to `implementing`, the human `project:owner` must confirm that each acceptance
criterion is covered at stage `planned`, the `implementation-plan` artifact is registered, and the
immutable container digest has been selected and recorded.
