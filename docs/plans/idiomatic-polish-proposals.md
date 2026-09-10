# Implementation plan: optional review-only idiomatic polish

> GitHub issue: [#128](https://github.com/Modern-Ash/renovatio/issues/128)  
> Agora work: `ai-modernization/idiomatic-polish-proposals`

## Delivery shape

Implement a bounded orchestration layer in `renovatio-provider-cobol` that accepts commit-bound
green prerequisite evidence and a candidate-generator seam, validates a returned candidate without
touching generated sources, and retains only deterministic `proposal.patch` and `manifest.json`
artifacts. Reuse the accepted provider guardrail types and manual-action writer. The generator seam
keeps provider/cache execution outside deterministic recipes and permits provider-call sentinels in
offline tests.

No OpenRewrite recipe, COBOL parser, base IR type, or annotated sidecar is mutated by this work.

## Planned contracts

Add package `org.shark.renovatio.provider.cobol.polish` with small immutable contracts:

- `PolishProposalFamily`: the four closed families from the specification;
- `PolishPrerequisiteEvidence`: commit, baseline, selectors, commands, hashes, environment, and
  green prerequisite results;
- `PolishProposalRequest`: bounded generated root, generated sources, affected nodes, family,
  accepted semantic projections, and prerequisite evidence;
- `PolishCandidate`: normalized unified diff, proposed output hashes, changed paths, public
  signatures, family payload, and provenance returned by the generator seam;
- `PolishCandidateGenerator`: one functional interface and the only candidate-generation call;
- `PolishProposalManifest`: canonical retained metadata with review state fixed to `PROPOSED`;
- `PolishProposalOutcome`: `INELIGIBLE`, `FAILED`, or `ELIGIBLE_FOR_REVIEW`, never accepted/applied;
- `IdiomaticPolishService`: fail-fast admission, one generator invocation, isolated validation,
  artifact retention, discard, and action-item aggregation;
- `PolishArtifactWriter`: atomic deterministic writes beneath the report root only;
- `PolishActionItemFactory`: stable `manual-action-item.v1` failures keyed by proposal identity.

Add strict schema
`renovatio-provider-cobol/src/main/resources/schema/idiomatic-polish-proposal.v1.schema.json` and
register it through the existing schema catalog. Canonical serialization uses sorted paths and map
keys, UTF-8, LF, lowercase SHA-256, and no timestamps.

## Gate behavior

### Pre-generation admission

Validate the request and require the current commit/baseline, nonempty affected selectors, complete
input and behavior hashes, schema/compilation/characterization green values, stable deterministic
generation, and no unresolved error action. Return before the generator seam on every failure.

### Candidate validation

Validate in order through the existing `GuardrailGateRunner`:

1. strict manifest/family/unified-diff/path schema;
2. injected Java 17 compilation check over a disposable candidate tree;
3. injected affected-characterization check over that same disposable tree;
4. existing bounded/reproducible/provenance/public-signature review-eligibility validation.

The service never applies a patch to the supplied generated root. The disposable candidate
materializer is an injected validation boundary used only by tests and build adapters, with a
read-only original-tree hash checked before and after.

### Success

Write `proposal.patch` and canonical `manifest.json` to a temporary sibling directory, fsync/close,
then atomically move it to
`build/reports/renovatio/idiomatic-polish/<proposal-id>/`. Return metadata only.

### Failure

Delete the temporary candidate directory, retain no proposal directory, upsert one stable failure
item into the current run's in-memory action collection, preserve unrelated current-run items, and
atomically rewrite `manual-action-items.json`. Never load stale disk-only items into the collection.

## Family validation

- Naming: legal Java target, exact node/current symbol/references, collision-free, owner-approved
  public signature when relevant.
- Port: one dependency boundary and interface, bounded call sites, identical arguments, return,
  exceptions, invocation count, and error behavior; no dependency/config/transport additions.
- Strategy: one conditional region, exhaustive existing branches, identical predicates, ordering,
  default, state mutation, and errors.
- Flags: exact fields and observed combinations, deterministic proof of mutual exclusion and
  exhaustiveness, and one-to-one typed-state mapping.

Represent family payloads as a closed Jackson-polymorphic contract so unknown families and unknown
fields fail schema validation.

## Tests-first sequence

1. Add contract tests for all request/evidence/family invariants and the strict schema.
2. Add admission tests proving every red/stale prerequisite yields zero generator calls and no
   artifact.
3. Add family tests for one accepted and multiple rejected payloads per family.
4. Add artifact tests for canonical bytes, content-addressed identity, path confinement, atomic
   replacement, and repeated-run equality.
5. Add orchestration tests forcing schema, compilation, characterization, and review failures and
   proving fail-fast order, candidate discard, unchanged original hashes, stable action items, and
   preservation of unrelated current-run items.
6. Add success integration coverage producing an eligible review package while exposing no apply
   API or source-tree write.
7. Extend architecture tests to prove recipes do not depend on the polish package or provider/LLM
   runtime types.

## Verification commands

Focused development:

```bash
mvn -q -pl renovatio-provider-cobol -am test -Djacoco.skip=true
```

Affected reactor and accepted guardrails:

```bash
mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes,renovatio-llm -am clean test -o -Djacoco.skip=true
```

The final lane repeats the affected reactor with Java 17 and Maven 3.9.12 in the repository's
pinned network-disabled container. The report records exact commands, module totals, fixture
selectors, hashes, forced failures, zero-call sentinels, and original-tree before/after equality.

## Acceptance-criterion coverage

| Criterion | Planned proof |
| --- | --- |
| `diff-only` | Canonical artifact writer, no apply API, no source-tree writer, and repeated-byte tests. |
| `eligible-only` | Pre-admission evidence validator and zero-call sentinel tests for every prerequisite. |
| `human-gate` | Outcome/review state closed to `PROPOSED` and architecture/API tests excluding accept/apply operations. |
| `discard-on-failure` | Ordered forced-gate tests, unchanged hashes, absent artifact directory, and stable merged current-run action report. |

## Governed artifacts and handoff

Register this plan as `implementation-plan` and the human policy as `review-policy`. After the Spec
Owner marks every criterion `planned`, transition to `planned`, then to `implementing`. Register a
commit-bound test report and successful evidence before `verified`; final acceptance remains with
`project:owner`.
