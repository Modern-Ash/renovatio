# Consolidated specification: LLM runtime, catalog, and governed cache

> Authority for issue #125 and Agora work `ai-modernization/llm-runtime-catalog-cache`.
> This v2 consolidation supersedes the evolving v1 URI while preserving its immutable history.

## Scope and boundaries

`renovatio-llm` enriches only residual semantic cases: domain naming, irreducible control-flow
plans, `REDEFINES` intent, `OCCURS DEPENDING ON` intent, and unsupported-construction explanations.
Deterministic COBOL parsing and OpenRewrite recipes never call a provider. The existing NQL-only
service is named `CobolNqlParsingService` and remains outside provider/runtime responsibilities.

## Prompt catalog

The catalog contains exactly five versioned YAML prompts, one for each supported selector. Every
entry requires a versioned `promptId`, known `appliesTo`, nonblank `system`, nonempty ordered
`fewShot`, versioned JSON `outputSchema`, ordered known validators, and a fallback resource. Unknown
fields, duplicate/unversioned IDs, unknown selectors/validators, or missing resources fail startup.

The model-output validators are `json-schema.v1`, `annotated-ir-reference.v1`,
`public-signature-preservation.v1`, and `sanitized-persistence.v1`. Fallback rendering is a separate
failure path. Each fallback resource has exactly `fallbackVersion: renovatio-llm-fallback.v1`,
`type: MANUAL_ACTION`, a stable `LLM_*` `diagnosticCode`, and nonblank `manualAction`; invalid or
unknown fields fail startup. Runtime output uses that declared diagnostic and retains the triggering
failure category separately.

## Provider runtime

The provider-neutral interface has an Anthropic Messages adapter and offline fake. Anthropic reads
only `ANTHROPIC_API_KEY`, uses the configured model, temperature zero, 60-second timeout, and at most
three attempts with capped full jitter for retryable timeout/rate-limit/server failures. Credentials,
headers, raw prompts, raw IR, and unrestricted provider envelopes are never persisted.

Cache lookup happens before provider construction. On a miss, provider configuration preflight
happens before `AttributionGateway.begin`; missing model/credentials fail locally with
`PROVIDER_CONFIGURATION_INVALID`, without attribution or network activity. A verified hit constructs
no provider and starts no runtime miss attribution.

## Validation, fallback, and persistence

Provider output passes strict JSON/schema validation, ordered catalog validators, issue #124
annotated-IR semantic validation, and allowlist sanitization. Any provider, schema, semantic, or
sanitization failure produces deterministic fallback metadata, the catalog manual action, and the
existing deterministic result—never a model-authored proposal.

Cache identity hashes canonical input, prompt ID/version, schema ID/hash, ordered validators,
provider, model, and `renovatio-llm.v1`. Envelopes separate `resultDisposition`
(`MODEL_SUCCESS`/`DETERMINISTIC_FALLBACK`) from `promotionDisposition`
(`PENDING_PROMOTION`/`COMMITTED`/`INVALID_ATTRIBUTION`). Only valid `COMMITTED` envelopes proven by
the Git-derived index and verified manifest can satisfy lookup.

## Agora attribution and reconciliation

Every miss runs through `llm-enrichment/enrich`, binding prompt/provider/model, input/output/schema/
cache hashes, runtime contract, dispositions, artifact URI, and sanitized failure category. Failure
to initialize attribution prevents the provider call. Observable in-process completion failure
quarantines the candidate as `INVALID_ATTRIBUTION`.

Agora persists the tool result after process exit. A retrospective persistence failure therefore
leaves the sanitized candidate `PENDING_PROMOTION`, lookup- and promotion-ineligible. Reconciliation
requires durable completed `RUN.md` and `RESULT.md`, exit code zero, nonempty result, and exact
identity/hash/disposition matches. Missing, failed, empty, or mismatched results never satisfy
`agora-attribution` and block promotion. An outer cache-hit invocation may exist but its empty result
is verification history, not miss attribution.

## Four-commit promotion

Promotion is exactly:

1. Commit A: validated envelope changed to `COMMITTED`, rehashed, and committed.
2. Commit B: technical index generated exclusively from Commit A and committed.
3. Commit C: owner approval plus Agora cache-promotion evidence binding A/B and hashes.
4. Commit D: only the generated digest-bound verified manifest.

Build/runtime verification discovers the commit that introduced the manifest, requires its changed
paths to equal only the manifest path, verifies C is an ancestor of D and D of `HEAD`, and compares
the manifest bytes at D with loaded authority. It also verifies A/B/C ancestry, exact changed paths,
envelope/index hashes, owner approval/evidence, report digest, and reconciled miss attribution.
Empty authority is permitted only when both index and manifest entries are empty. Lookup eligibility
begins at D; working-tree, pending, quarantined, mismatched, unapproved, or A/B/C-only entries fail
closed.

## Acceptance

- Five strict prompt entries and strict fallback resources load; every malformed class fails closed.
- Claude configuration, retry/timeout policy, and offline fake are deterministic and testable offline.
- Identical complete identities produce equal keys; any identity change invalidates the key.
- A verified hit performs zero provider construction/calls and zero runtime attribution calls.
- A miss produces validated output or explicit fallback, durable reconciled attribution, and a
  sanitized pending candidate.
- A/B/C/D and all content/ancestry/path bindings are required before lookup.
- Secrets and unrestricted source/provider content are absent from cache, logs, diagnostics, and
  Agora records.
- Focused and full Java 17 reactors, consistency, review, and owner approval must be successful and
  commit-bound before completion.
