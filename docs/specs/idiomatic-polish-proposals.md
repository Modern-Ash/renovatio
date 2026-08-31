# Specification: Optional review-only idiomatic polish

> GitHub issue: [#128](https://github.com/Modern-Ash/renovatio/issues/128)  
> Agora work: `ai-modernization/idiomatic-polish-proposals`

## 1. Outcome and boundary

Produce optional, reviewable Java refactoring proposals only after deterministic COBOL
transliteration and its non-negotiable guardrails are green. A proposal is an inert artifact: the
polish service may inspect validated semantic inputs and generated Java, but it never mutates COBOL,
IR, sidecars, generated sources, the repository, or a workspace source tree.

The service has no apply operation, endpoint, callback, or implicit write path. Applying an accepted
proposal is a separate human-controlled activity outside the LLM execution path and outside this
work item. Deterministic output remains the authoritative delivered output whether proposal
generation succeeds, fails, or is declined.

This specification is authoritative for issue #128. It preserves the contracts delivered by issues
#122 through #127 and cannot weaken their schema, compilation, characterization, review,
attribution, cache, fallback, or recipe-purity requirements.

## 2. Authoritative dependencies

| Dependency | Required contract |
| --- | --- |
| Guardrails | `docs/specs/characterization-guardrails.md`; immutable gate order `schema` -> `compilation` -> `characterization` -> `review-eligibility`. |
| Deterministic output | The generated Java and base IR produced by the deterministic lane, identified by lowercase SHA-256 hashes and a commit-bound characterization baseline. |
| Annotated IR | `docs/specs/annotated-ir-contract.md` and a schema-valid `cobol-annotated-ir.v1` sidecar when one is supplied. |
| Residual enrichment | `docs/specs/residual-semantic-enrichment.md`; provider-neutral execution, content-addressed cache identity, prompt provenance, and governed Agora attribution. |
| Annotated pass | `docs/specs/annotated-openrewrite-pass.md`; accepted annotations may already be reflected in generated Java, but polish never changes annotation review state. |
| Manual actions | `manual-action-item.v1` and the atomic report at `build/reports/renovatio/manual-action-items.json`. |

Missing, red, stale, or mismatched dependency evidence fails closed before a provider call. A
confidence score, cached prose, or a previously green unrelated commit cannot replace current
commit-bound evidence.

## 3. Closed proposal scope

A request selects exactly one of these proposal families. Inputs that do not match exactly one
family are ineligible and produce no proposal.

| Family | Permitted suggestion |
| --- | --- |
| `DOMAIN_NAMING_REFINEMENT` | Refine generated Java names using accepted domain terminology without changing behavior. |
| `PORT_EXTRACTION` | Suggest Java interfaces and call-site changes that expose an existing external dependency as a port. |
| `STRATEGY_EXTRACTION` | Suggest a strategy boundary for already characterized conditional behavior. |
| `FLAG_COLLAPSE` | Suggest replacing a characterized family of related flags with a typed state representation. |

Each family has a closed, schema-enforceable contract:

- `DOMAIN_NAMING_REFINEMENT` identifies one exact IR node and current Java symbol, proposes one
  legal non-keyword identifier, enumerates every same-compilation-unit reference to rename, and
  proves the target does not collide in scope. An unapproved public-signature rename is invalid.
- `PORT_EXTRACTION` identifies one existing concrete external-dependency boundary, proposes one
  interface plus bounded generated-Java call-site updates, and preserves argument order, return
  type, declared exceptions, invocation count, and observable error behavior. It cannot add a
  dependency, configuration key, transport, retry, or business rule.
- `STRATEGY_EXTRACTION` identifies one characterized conditional region, proposes an interface and
  exhaustive implementations for its existing branches, and preserves predicate meaning, branch
  order, default behavior, state mutation order, and observable errors. No branch may be invented,
  combined, dropped, or made reachable under a new condition.
- `FLAG_COLLAPSE` identifies the exact related fields and every observed combination, and is valid
  only when characterization and deterministic data-flow facts prove the flags form one mutually
  exclusive, exhaustive state. Its typed representation maps every valid combination one-to-one;
  independent, overlapping, unknown, or invalid combinations make the request ineligible.

The service must not propose new business rules, alter COBOL arithmetic or data-layout semantics,
restructure uncharacterized control flow, resolve an unsupported construct speculatively, change
annotation review state, or claim behavioral equivalence without the required evidence.

## 4. Admission before proposal generation

Proposal generation is allowed only when one evidence set, bound to the current deterministic input
hashes and repository commit, proves all of the following:

1. **Schema is green.** The base IR, any annotated sidecar, the proposal request, and the manual
   action report contract validate against their exact committed schema versions with no unknown
   fields or unresolved node identities.
2. **Compilation is green.** The deterministic generated Java compiles with Java 17 and Maven
   3.9.x. The repository baseline is
   `mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am -DskipTests package`.
3. **Characterization is green.** The affected fixtures and observable behavior pass with
   `mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test`, and the evidence names
   the exact baseline reference and output hashes.
4. **Transliteration is stable.** Repeating deterministic generation from the same canonical inputs
   produces the same Java hashes and no unresolved error-level action item for the requested unit.

All four checks occur before prompt lookup, cache-miss attribution, provider construction, or
provider execution. An ineligible request returns a stable diagnostic and action item without
creating a candidate patch.

The request names the exact affected characterization scope as an ordered nonempty set of fixture
IDs and/or fully qualified test selectors. Its evidence records the repository commit, baseline
reference, exact command and Java/Maven environment, generated-input hashes, expected-behavior
artifact hashes, and successful result for every named selector. Candidate validation must rerun
that same scope; broader evidence may supplement but cannot replace an omitted affected selector.
Every declared changed path and affected IR node must map to at least one named selector.

## 5. Inspection and file boundaries

The service may inspect only the bounded inputs named by the request:

- schema-valid canonical base IR projections for the affected nodes;
- accepted annotated-IR projections for those nodes, when present;
- deterministic generated Java compilation units listed in the request;
- commit-bound gate results, hashes, diagnostics, and proposal provenance needed to validate the
  request.

It must not inspect or persist raw COBOL source, credentials, provider headers, hidden reasoning,
unrelated repository files, unrelated IR nodes, or arbitrary workspace content.

A suggested diff may create or update `.java` files only beneath the request's declared generated
Java output root. It may not include COBOL files, base or annotated IR, schemas, prompts,
configuration, tests, fixtures, reports, build files, repository metadata, or paths outside that
root. Path traversal, absolute paths, symbolic-link escapes, undeclared paths, and whole-file
deletions fail closed.

## 6. Reviewable artifact contract

An eligible proposal is written beneath:

`build/reports/renovatio/idiomatic-polish/<proposal-id>/`

The directory contains exactly:

- `proposal.patch`: a normalized UTF-8 unified diff with LF line endings, deterministic path and
  hunk ordering, and no timestamps or absolute paths;
- `manifest.json`: canonical JSON conforming to `idiomatic-polish-proposal.v1`.

`proposal-id` is content-addressed from the schema version, family, canonical bounded inputs,
prompt version, deterministic baseline hashes, and normalized patch hash. Identical inputs and
runtime identity produce byte-identical artifacts.

The manifest records at least:

- schema version, proposal ID, proposal family, creation-free content identity, and review state
  `PROPOSED`;
- base IR, sidecar when present, generated-input, and patch SHA-256 hashes;
- declared created or updated paths plus input and proposed-output hashes for every changed path;
- the commit and characterization baseline, executed gates, diagnostic references, and evidence
  artifact hashes;
- prompt ID/version, output-schema hash, validators, cache identity, provider/model identity, and
  Agora tool-run reference when provider execution occurred;
- detected public-signature changes and the explicit owner approval references required before the
  proposal can be review-eligible.

The artifact contains no executable apply instruction. The service returns artifact metadata and
paths only. It never feeds `proposal.patch` to Git, OpenRewrite, `patch`, an IDE, or a workspace
writer.

## 7. Candidate validation and retention

Every generated candidate is validated in a disposable isolated copy of the declared generated
Java tree. The original workspace is read-only for the entire operation. Validation runs in the
same immutable order as the accepted guardrail contract:

1. **Schema:** validate the response, canonical manifest, unified-diff syntax, paths, hashes, and
   family-specific constraints.
2. **Compilation:** apply the candidate only inside the disposable copy and compile with Java 17.
3. **Characterization:** run the affected characterization suite against the disposable copy and
   compare every observable result with the accepted baseline.
4. **Review eligibility:** prove the changed paths are declared and inside the generated root,
   hashes are complete, provenance is complete, a second generation is byte-identical, and every
   public-signature change has explicit `project:owner` approval.

Validation fails on any schema diagnostic, malformed or unapplicable hunk, undeclared or forbidden
path, missing hash or provenance value, compilation error, test failure, behavioral mismatch,
non-reproducible output, unapproved public-signature change, attempted workspace write, or provider
or attribution failure.

Passing validation makes the artifact eligible for human review; it does not apply or accept it.
Only `project:owner`, acting outside the LLM execution path, may decide whether a patch should be
applied in separate governed work.

## 8. Failure, discard, and manual action items

At the first failed admission or candidate-validation gate:

1. stop later gates;
2. discard the candidate and its disposable tree;
3. retain the deterministic generated output byte-for-byte;
4. write or replace the atomic `manual-action-item.v1` report at
   `build/reports/renovatio/manual-action-items.json`;
5. return no reviewable patch artifact for the failed proposal.

The action item uses stable diagnostic family `COBOL-POLISH-<GATE>-FAILED`, identifies the proposal
family and affected source/program/node, records the exact failed gate and diagnostic reference,
states that deterministic Java was retained, and names the human action and evidence needed for a
new request. Provider, prompt, cache, output-hash, and Agora references are included when execution
reached those boundaries; secrets and raw provider output are forbidden.

Discard evidence includes the original generated-tree hash before the request, the same hash after
failure, absence of a retained proposal directory, the action-report hash, the executed-gate list,
and a test proving that no workspace writer or automatic apply path was invoked.

The report represents the complete current orchestration run. Failure handling upserts the one
stable item for the same proposal ID into the run's in-memory action-item collection, preserves
unrelated items produced by that current run, sorts the complete collection deterministically, and
atomically replaces the report. It must not merge disk-only items from an earlier run, so obsolete
warnings cannot survive a clean rerun.

## 9. Human review policy

Automation may report only `ineligible`, `failed`, or `eligible-for-review`. It cannot report
`accepted`, `approved`, `applied`, or `semantics-preserving` on behalf of a human.

The required `review-policy` artifact must define the human checklist for bounded paths, behavior,
public API impact, provenance, evidence freshness, and explicit acceptance or rejection. Approval
of that document does not approve an individual proposal. Each proposal requires its own decision
outside the generation request, and rejection leaves deterministic output unchanged.

## 10. Acceptance scenarios

### `diff-only`

- Every success produces only canonical `proposal.patch` and `manifest.json` artifacts.
- Architecture and integration tests prove there is no automatic apply API or source-tree writer.
- Repeated requests with identical inputs produce byte-identical artifacts.

### `eligible-only`

- A green commit-bound schema, compilation, characterization, and transliteration evidence set is
  required before any provider or cache-miss path executes.
- A forced red or stale value for each prerequisite produces zero provider calls and no patch.

### `human-gate`

- Passing validation yields only `eligible-for-review` and `PROPOSED` metadata.
- Tests prove no AI actor, runtime response, cache hit, or validation result can accept or apply the
  patch.

### `discard-on-failure`

- A forced failure at every candidate gate stops later checks, retains the original generated-tree
  hash, removes the candidate, and emits exactly one schema-valid stable action item.
- No failed case leaves a proposal directory or modifies generated Java.

## 11. Required artifacts and evidence

- this specification at
  `repo://docs/specs/idiomatic-polish-proposals.md`, registered before transition to `clarified`;
- a `review-policy` artifact defining the separate human decision contract, registered before
  implementation begins;
- an implementation plan registered before transition to `planned`;
- a successful test report before verification and completion.

The test report must bind the final implementation commit, Java/Maven environment, exact commands,
per-module totals, forced failures for every prerequisite and candidate gate, reproducibility
hashes, provider-call sentinels, workspace before/after hashes, and the network-disabled offline
lane. Focused and full affected reactors must pass with Java 17 and no failed or skipped gate tests.

The human actor assigned the swarm's `spec-owner` role, `project:owner`, confirmed on 2026-08-31
that the specification URI is registered, all clarification questions are resolved, and all four
acceptance criteria are sufficiently specified for the `spec-clarified` gate. This confirmation is
scope acceptance only; it is not implementation acceptance and does not approve any future patch.

## 12. Acceptance mapping

| Agora criterion | Specification obligation |
| --- | --- |
| `diff-only` | Sections 1, 6, and 10 define inert deterministic patch artifacts and prohibit an apply path. |
| `eligible-only` | Sections 4 and 7 define commit-bound preconditions and fail-fast validation. |
| `human-gate` | Sections 1, 7, 9, and 10 reserve every acceptance/application decision for explicit human review. |
| `discard-on-failure` | Sections 7, 8, and 10 define candidate disposal, unchanged output evidence, and stable action items. |

## 13. Out of scope

- Automatically applying, committing, merging, or deploying a proposed patch.
- Editing COBOL, base IR, annotated sidecars, schemas, prompts, tests, fixtures, or configuration as
  part of a proposal.
- Replacing deterministic translation or accepted annotation application.
- Claiming that a proposal preserves semantics solely because it compiled or came from a cache.
- Defining the separate governed work that a human may later use to apply an accepted patch.
