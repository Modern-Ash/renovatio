# Specification: Residual LLM semantic enrichment

> GitHub issue: [#126](https://github.com/Modern-Ash/renovatio/issues/126)
> Agora work: `ai-modernization/residual-semantic-enrichment`

## 1. Outcome and boundary

Produce reviewable annotations only for semantic work that cannot be derived reproducibly from the
supported COBOL IR. The LLM may suggest domain language, plans for irreducible control flow, data
layout intent, and explanations for unsupported constructs. It never becomes the semantic authority,
never mutates the base IR, never applies an OpenRewrite recipe, and never processes constructs owned
by the deterministic lane.

This specification is authoritative for issue #126. Planning and implementation must preserve the
contracts from issues #122, #124, and #125 and the characterization gates named below.

## 2. Authoritative dependencies

| Dependency | Required contract |
| --- | --- |
| Annotated IR | `docs/specs/annotated-ir-contract.md`, schema `cobol-annotated-ir.v1`, and `renovatio-cobol-ir/src/main/resources/schema/cobol-annotated-ir.v1.schema.json`. |
| LLM runtime | `docs/specs/llm-runtime-catalog-cache-v2.md`, runtime identity `renovatio-llm.v1`, catalog `prompts/catalog-v1.yaml`, committed cache authority, and governed Agora attribution. |
| Characterization | `docs/specs/characterization-guardrails.md` and `docs/test-plans/characterization-guardrails.md`. A proposal may be generated before these gates complete, but no control-flow proposal is eligible for downstream application without green schema, compilation, characterization, and review gates in that order. |

Unsupported or mismatched versions fail closed to deterministic translation plus a manual action.
No best-effort conversion between contract versions is permitted.

## 3. Closed routing classification

The enrichment router is deterministic and closed. It accepts a validated base IR and exact node
identity, then returns either one residual family or `DETERMINISTIC`. It does not inspect generated
natural-language text or call a provider to decide routing.

| Route | Eligible input |
| --- | --- |
| `DOMAIN_NAMING` | Paragraph or data-item identity for which a reviewable Java/domain name or bounded-context suggestion was explicitly requested. |
| `CONTROL_FLOW_PLAN` | A control-flow component already classified deterministically as irreducible and containing a `GO TO` edge. |
| `DATA_INTENT.REDEFINES` | A data item with parsed `REDEFINES` metadata whose business intent is not represented by deterministic layout semantics. |
| `DATA_INTENT.OCCURS_DEPENDING_ON` | A data item with parsed `OCCURS DEPENDING ON` metadata whose business intent is not represented by deterministic bounds semantics. |
| `UNSUPPORTED_EXPLANATION` | A stable parser/transpiler diagnostic explicitly classified as unsupported. |
| `DETERMINISTIC` | Every supported deterministic construct and every input not matching exactly one residual rule. |

`MOVE`, `COMPUTE`, `IF`, `EVALUATE`, simple `PERFORM`, basic PIC mapping, level-88 conditions,
reducible control flow, and deterministic `REDEFINES`/`OCCURS DEPENDING ON` layout facts always route
to `DETERMINISTIC`. That route returns without provider construction, cache-miss attribution, prompt
lookup, or network activity. Tests use failing provider/attribution suppliers and assert zero calls.
Ambiguous or multiply matching routes fail closed to `DETERMINISTIC` with a stable manual action.

For `DOMAIN_NAMING`, an explicit request is a governed orchestration request containing the exact
`nodeId`, annotation family, collision scope, public-signature disposition, and Agora tool-run
identity. Merely encountering a paragraph or data item does not opt it into naming enrichment.

## 4. Common request, output, and provenance contract

Every residual request contains:

- `schemaVersion: residual-enrichment-request.v1`;
- `baseIrVersion`, `baseIrHash`, `nodeId`, and `nodeKind` from the validated annotated-IR contract;
- exactly one closed route and its route-specific structured input;
- `promptId`, output-schema identifier/hash, ordered validators, provider, and model;
- canonical input hash and the characterization baseline reference when the family requires it.

Raw COBOL source, credentials, headers, unrestricted provider envelopes, and unrelated IR nodes are
forbidden. The provider receives the smallest canonical IR projection required by the selected
family.

Successful output is one typed `AnnotatedCobolModel` v1 annotation with:

- the exact target node identity and family-specific payload;
- confidence and `reviewState` as defined by the annotated-IR contract;
- provider/model/prompt/schema/input/output hashes, tool-run reference, and cache disposition;
- `PROPOSED` or `NEEDS_REVIEW` only; enrichment cannot emit `ACCEPTED` or `REJECTED`.

Provider, schema, semantic, sanitization, cache, or attribution failure returns the deterministic
result and a catalog-owned manual action. A failed output is never persisted as a model proposal.

## 5. Family contracts

### 5.1 Domain language

Input contains the node's current COBOL identifier, node kind, canonical local semantic projection,
and optionally the parent paragraph/data hierarchy. Output is `DOMAIN_NAMING` with nonblank
`suggestedName` and `rationale`, plus optional nonblank `boundedContext`.

An acceptable suggestion must:

- be a legal Java identifier after the project's deterministic identifier normalization;
- not collide, case-insensitively, with another name in the supplied scope;
- preserve the node reference and public-signature constraints;
- include a rationale tied to supplied IR facts, not invented business behavior;
- remain `PROPOSED` until reviewed.

Collision scope is the complete symbol table of the same COBOL program: paragraphs compare against
all paragraph/generated method names, while data items compare against their containing record and
all generated members visible in that program. Comparison uses the project's deterministic Java
identifier normalization and is case-insensitive. The public-signature disposition is derived from
the base IR: `LINKAGE SECTION`, `ENTRY`, externally referenced interfaces, and already published
signatures are protected. Their suggestions remain advisory and cannot rename a signature
automatically.

### 5.2 Irreducible GO TO plan

Input contains the deterministic control-flow component, ordered nodes/edges, entry/exit nodes,
source paragraph identities, and characterization baseline reference. Output is
`CONTROL_FLOW_PLAN` with nonempty `affectedNodeIds`, ordered nonempty `steps`, and nonempty `risks`.

The affected IDs must resolve inside the supplied component. A plan is advisory and cannot modify
IR or source. Downstream eligibility requires, in order: schema validation, compilation of the
candidate transformation, green characterization tests against the referenced baseline, and human
review. Missing or red characterization gates discard the proposal rather than persisting an
applicable annotation. Orchestration retains only the deterministic fallback, records stable
diagnostic `LLM_CHARACTERIZATION_NOT_GREEN`, and emits a manual action to restructure the identified
component manually or restore a green characterization baseline before requesting another plan.

### 5.3 REDEFINES and OCCURS DEPENDING ON intent

Input contains only the parsed layout relationship, referenced node identities, deterministic type
and bounds information, and local usage facts. Output is `DATA_INTENT` with exact `construction`,
nonblank `interpretation`, and nonempty `assumptions`.

These annotations are always created as `NEEDS_REVIEW`. Human confirmation is represented solely by
an annotated-IR review transition to `ACCEPTED` or `REJECTED`, with nonblank `reviewedBy` and RFC 3339
UTC `reviewedAt`. The reviewer may not rewrite proposal identity or payload in place; a changed
interpretation is a new proposal. Without confirmation, or after rejection, no downstream recipe may
consume the interpretation. Deterministic layout semantics remain available independently.

### 5.4 Unsupported construction explanation

Input contains the stable unsupported diagnostic, construction kind, node identity, and minimal
canonical semantic projection. Output is `UNSUPPORTED_EXPLANATION` with nonblank `construction`,
`explanation`, and `manualAction`.

An actionable manual item identifies the exact node/construction, explains why deterministic
translation is unavailable, names the semantic risk, and states a concrete human action and the
evidence required to close it. It must not claim that behavior was preserved or invent missing
business rules.

## 6. Prompt and validation policy

The five prompt entries established by issue #125 remain the only allowed prompts:

- `cobol.domain.naming.v1`;
- `cobol.goto.restructure.v1`;
- `cobol.redefines.intent.v1`;
- `cobol.occurs-depending.intent.v1`;
- `cobol.unsupported.explain.v1`.

Each request must select the prompt matching its deterministic route. Outputs pass the catalog's
strict JSON schema, annotated-IR reference validation, public-signature preservation, and sanitized
persistence policy. Temperature is zero. Cache and Agora tool-run attribution use the complete
identity contract from issue #125. No prompt is embedded in a recipe or invoked from CI recipes.

## 7. Stable failures and manual actions

The implementation must expose stable categories for at least: unsupported contract version,
unresolved node, route mismatch, deterministic-only route, schema rejection, characterization not
green, human confirmation required/rejected, provider failure, and attribution failure. Diagnostics
contain identifiers and hashes but no raw source or secrets.

Failure behavior is deterministic:

1. retain the unmodified deterministic IR/result;
2. discard or quarantine any ineligible model proposal;
3. emit the catalog fallback diagnostic and precise manual action;
4. retain governed attribution for any cache miss that reached provider execution.

## 8. Acceptance evidence

- Domain-name and bounded-context fixtures prove legal, collision-free, provenance-bearing proposals.
- An irreducible `GO TO` fixture produces a structured plan while reducible control flow produces no
  provider or attribution call; red characterization evidence makes the plan ineligible.
- `REDEFINES` and `OCCURS DEPENDING ON` fixtures remain `NEEDS_REVIEW`; absent/rejected confirmation
  is unusable downstream and accepted confirmation contains human identity and timestamp.
- Unsupported fixtures produce precise manual items without claiming translated semantics.
- Every supported deterministic construct listed in section 3 is tested with provider and
  attribution sentinels that fail if invoked.
- Focused and full Java 17 reactors pass with registered commit-bound evidence before completion.

The focused command is
`mvn -pl renovatio-llm,renovatio-cobol-ir -am test`. The full command is `mvn test`. Both run with
Java 17, must exit zero with no test failures or errors, and their registered reports must contain
the exact tested commit SHA, command, module scope, totals, and environment. The full-reactor
evidence must bind the final implementation commit or an ancestor followed only by governance or
documentation changes that are explicitly enumerated.

## 9. Required governed artifacts

- this `spec` before transition to `clarified`;
- the existing versioned `prompt-catalog`, registered as authority before transition to `clarified`;
- an implementation plan before `implementing`;
- focused and full-reactor `test-report` artifacts before verification/completion.

Artifact registration is append-only. Re-registering this same URI after an approved clarification
records a newer content digest; the latest registered digest is the single current authority and
earlier digests are immutable superseded history, not competing specifications.

The human actor assigned the swarm's `spec-owner` role is the only authority that may perform an
annotated-IR `ACCEPTED` or `REJECTED` transition for `DATA_INTENT` proposals or satisfy the human
review gate for a `CONTROL_FLOW_PLAN`. For this swarm that actor is `project:owner`. AI agents,
provider runtimes, cache hits, tests, and the developer role cannot supply that decision. The Spec
Owner's approval of this section confirms that all five work acceptance criteria are complete and
unambiguous for transition to `clarified`; it is not an implementation or completion approval.
