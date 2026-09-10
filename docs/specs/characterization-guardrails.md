# Characterization Harness and Non-Negotiable Gates

> GitHub issue: [#122](https://github.com/Modern-Ash/renovatio/issues/122)  
> Agora work: `ai-modernization/characterization-guardrails`  
> Lifecycle stage: specification

## 1. Purpose

Define the executable safety contract that every deterministic or LLM-assisted COBOL modernization
change must satisfy. This specification establishes the fixture corpus, ordered validation gates,
offline CI boundary, review eligibility, and deterministic fallback record. It does not implement
new COBOL semantics or an LLM provider.

This document is the `spec` artifact required to move the work from `drafting` to `clarified`. It is
complete when the fixture matrix, expected outputs, gate commands, review authority, offline lane,
and fallback action-item contract below are all present and mapped to the work criteria.
`repo://docs/specs/characterization-guardrails.md` is the registered artifact; registration must
exist before the `spec-clarified` transition is attempted.

## 2. Invariants

- OpenRewrite recipes and deterministic translators never perform network or LLM calls.
- The LLM is never the sole source of semantics.
- A proposal is consumed only after all gates pass in the declared order.
- A failed gate leaves generated code unchanged, uses deterministic transliteration where possible,
  and emits a traceable manual action item.
- Identical source, configuration, schemas, and committed cache entries produce byte-identical
  outputs.

## 3. Golden-fixture corpus

Fixtures live under
`renovatio-provider-cobol/src/test/resources/characterization/<fixture-id>/`. Each fixture contains
the smallest source needed to expose one behavior and carries these committed expectations:

- `input.cob`: canonical COBOL input;
- `expected-ir.json`: canonical base IR when parsing is supported;
- `expected.java`: deterministic Java output when translation is supported;
- `expected-behavior.json`: observable inputs, outputs, state changes, and diagnostics;
- `expected-action-items.json`: expected fallback records, empty for fully supported fixtures.

The minimum passing corpus consists of these twelve named fixtures. Supported fixtures must contain
non-empty `expected-ir.json`, `expected.java`, and `expected-behavior.json`, with an empty
`expected-action-items.json`. Residual or unsupported fixtures must contain
`expected-behavior.json` and a non-empty `expected-action-items.json`; they contain
`expected-ir.json` only when the parser safely preserves the construct and intentionally omit
`expected.java` when no safe translation exists.

| Fixture id | Required coverage | Expected result |
| --- | --- | --- |
| `move-numeric` | Numeric `MOVE` and basic numeric `PIC` | Supported outputs |
| `move-alphanumeric-boundaries` | Alphanumeric `MOVE`, truncation and padding | Supported outputs |
| `compute-decimal-sign` | `COMPUTE`, signs, scale and decimal precision | Supported outputs |
| `if-nested` | Nested true and false `IF` paths | Supported outputs |
| `evaluate-level-88` | `EVALUATE` with level-88 conditions | Supported outputs |
| `perform-simple-nested` | Simple and nested structured `PERFORM` | Supported outputs |
| `goto-reducible` | Reducible `GO TO` control flow | Supported deterministic output |
| `goto-irreducible` | Irreducible `GO TO` control flow | No speculative Java; action item |
| `redefines-overlap` | `REDEFINES` with overlapping layouts | Preserved IR plus review action item |
| `odo-valid-boundary` | `OCCURS DEPENDING ON` at valid lower and upper bounds | Preserved IR plus review action item |
| `odo-invalid-count` | `OCCURS DEPENDING ON` outside declared bounds | Diagnostic and action item |
| `unsupported-construct` | Recognized but non-translatable construction | Diagnostic and action item; no Java |

For the `drafting` to `clarified` transition, defining this complete corpus and its file contracts
is sufficient. The twelve directories and expected-output files are implementation deliverables and
must exist before the corresponding criteria can advance beyond `specified`; they are not required
to clarify the specification.

Later issues may add fixtures, but they may not weaken or remove an existing expectation without a
reviewed specification revision.

## 4. Ordered admission gates

Every proposal runs through these gates, in order, stopping at the first failure:

1. **Schema validation** — validate base IR, annotated sidecar when present, and action-item output
   against their committed versioned JSON Schemas. Unknown fields or invalid types fail closed. The
   governing v1 files are `cobol-ir.v1.schema.json`, `annotated-cobol-model.v1.schema.json`, and
   `manual-action-item.v1.schema.json`. Issue #124 delivers the annotated-model schema; annotated
   output is ineligible until that schema is committed and active.
2. **Compilation** — compile generated Java with Java 17 and Maven 3.9.x. The baseline command is
   `mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am -DskipTests package`.
3. **Characterization** — run the fixture harness and affected module tests with
   `mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test`. Every committed output
   and observable behavior must match.
4. **Review eligibility** — confirm all previous gates passed, outputs are byte-reproducible, the
   diff is bounded to declared targets, provenance and content hashes are present, no public
   signature changed unexpectedly, and no unsupported construct was silently translated.

A proposal manifest must enumerate every file it may create, update, or delete. A bounded diff
touches only those paths and the fixture-specific generated-output directory; any other path fails
the gate. A public-signature change is expected only when the implementation plan lists the exact
fully qualified type and member signature, explains the compatibility effect, and records explicit
`project:owner` approval. Every other public API change is unexpected and fails closed.

Provider availability, persuasive prose, or an LLM confidence score cannot replace a gate.

## 5. Review authority

Automated checks determine eligibility only. The human actor `project:owner`, assigned to the Agora
`spec-owner` role, reviews the eligible diff and its evidence and holds final acceptance authority.
LLM-generated idiomatic polish requires a separate explicit human decision and is never
automatically applied. Rejection returns to deterministic output plus a manual action item; it does
not trigger an alternate unreviewed proposal.

## 6. Offline CI contract

Add a GitHub Actions Linux job using a pinned Java 17 distribution and Maven 3.9.x. Dependency
resolution and cache preparation may occur in a separate setup step. The governed test step then:

- runs Maven in offline mode with a pre-populated dependency cache;
- exposes no LLM API keys or provider credentials;
- strictly blocks unexpected network access and fails the job when an attempted connection is
  detected;
- executes deterministic translation and committed cache-hit scenarios;
- uploads test reports, generated diffs, and fallback action items as CI artifacts.

The offline step runs inside a container started with `--network=none`. Its evidence must include the
container invocation, a negative connectivity probe that attempts name resolution and an outbound
connection and proves both fail, confirmation that no provider credential variables are present,
the Maven `-o` execution log, and the resulting test report. A lane that merely avoids making a
request without enforcing network isolation does not satisfy this requirement.

The accepted toolchain baseline is Java 17 with Maven 3.9.12. The implementation plan must select a
Java 17 container image, resolve its immutable multi-architecture digest, and commit that digest in
the workflow before implementation begins. Selecting the registry digest is a planning artifact,
not a prerequisite for this specification to become `clarified`; execution with a mutable tag is
never acceptable.

A cache miss in the offline lane must not call a provider. It must fail closed or select the
deterministic fallback, according to the calling feature's contract.

## 7. Deterministic fallback and manual action items

Runtime action items are written to `build/reports/renovatio/manual-action-items.json`; matching
golden expectations remain committed with their fixtures. The report is registered as an Agora
artifact when used as lifecycle evidence. Each item contains:

- stable `id`;
- source file and COBOL location (`program`, division/section/paragraph, line or source span);
- IR node identity or source-content hash when available;
- construction family and concise reason for rejection;
- failed gate and diagnostic reference;
- deterministic fallback applied, or an explicit statement that no safe fallback exists;
- required human action and acceptance condition;
- severity and review status;
- schema, prompt, model, cache and output hashes when an LLM proposal was involved;
- Agora tool-run reference when an external call occurred.

The report must not contain credentials, raw provider headers, hidden reasoning, or unredacted
sensitive source beyond the bounded location needed to act.

When a safe deterministic transliteration exists, the failed proposal is discarded and that
transliteration is emitted. When no safe fallback exists, the system emits no transformed code for
the affected unit and records the manual action item instead; speculative output is forbidden.
For the existing `safe-fallback` acceptance criterion, this fail-closed refusal is the deterministic
fallback outcome: “deterministic transliteration” does not require fabricating transformed code when
no semantics-preserving translation exists.

## 8. Clarified dependency and artifact semantics

- Multiple registrations of `repo://docs/specs/characterization-guardrails.md` are revisions of one
  logical `spec` artifact. The last row in the governed artifact ledger at evaluation time is the
  authoritative content digest. The digest is intentionally not embedded in this file because doing
  so would make the document hash self-referential.
- This work may transition to `clarified` before issue #124 is delivered because it specifies the
  guardrail contract, not the annotated-model implementation. Annotated output remains categorically
  ineligible until `annotated-cobol-model.v1.schema.json` is committed and active.
- On 2026-08-30, the human `project:owner` explicitly resolved the remaining drafting questions and
  accepted this authoritative specification for the `drafting` to `clarified` decision.

## 9. Acceptance mapping

| Agora criterion | Specification obligation |
| --- | --- |
| `golden-fixtures` | Section 3 defines the required corpus and committed expected outputs. |
| `gate-order` | Section 4 defines the mandatory schema → compilation → characterization → review sequence. |
| `safe-fallback` | Section 7 defines fail-closed behavior and traceable manual action items. |
| `offline-ci` | Section 6 defines the credential-free, network-independent CI lane. |

## 10. Required delivery evidence

Implementation must later register a `test-plan` before execution and a successful `test-report`
covering every corpus family, the four ordered gates, a forced failure at each gate, deterministic
fallback, and the offline cache-hit lane. This specification alone does not claim implementation or
verification.
