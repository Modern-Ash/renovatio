# Specification: Governed LLM runtime, prompt catalog, and content-addressed cache

> GitHub issue: [#125](https://github.com/Modern-Ash/renovatio/issues/125)
> Agora work: `ai-modernization/llm-runtime-catalog-cache`

## 1. Outcome

Add a `renovatio-llm` module that enriches the accepted annotated COBOL IR through a
provider-neutral runtime, versioned prompts, strict validation, and a committed content-addressed
cache. Provider calls remain outside OpenRewrite recipes. Every cache miss is attributable through
Agora, while cache hits and deterministic fallbacks keep offline and CI execution reproducible.

The accepted dependency is the `cobol-annotated-ir.v1` contract produced by issue #124 and
specified in `docs/specs/annotated-ir-contract.md`. That Agora work is complete, so it does not
block this specification from transitioning to `clarified`.

## 2. Scope

This work includes:

- a provider-neutral Java client and a real Anthropic Claude adapter;
- an offline fake provider for deterministic tests;
- a versioned YAML `PromptCatalog` with strict loading and validation;
- canonical request identity and a repository-committed cache;
- deterministic fallback results for provider or validation failures;
- governed Agora attribution for each cache miss; and
- renaming the NQL-only `LlmIntegrationService` so it no longer claims to integrate an LLM.

This work does not apply LLM output through OpenRewrite recipes, automatically accept semantic
suggestions, or perform the optional idiomatic-polish pass. Those remain downstream work.

## 3. Prompt catalog contract

Every prompt is a versioned YAML resource with these required fields:

| Field | Contract |
| --- | --- |
| `promptId` | Stable dotted identifier such as `cobol.goto.restructure.v1`. The version suffix is mandatory. |
| `appliesTo` | Declarative selector over supported annotated-IR node kinds or construction types. |
| `system` | Nonblank system instruction. |
| `fewShot` | Nonempty ordered list of input/output examples. Empty lists are invalid in v1. |
| `outputSchema` | Bundled JSON Schema resource identifier with an immutable version. |
| `validators` | Ordered deterministic validator identifiers. |
| `fallback` | Deterministic fallback type and manual-action template. |

The catalog rejects duplicate IDs, unknown fields, missing referenced schemas, unknown validators,
unversioned IDs, and unsupported selectors at startup. Catalog resources are immutable once
published; semantic changes require a new prompt version.

The initial closed selector vocabulary is `DOMAIN_NAMING`, `CONTROL_FLOW_PLAN`,
`DATA_INTENT.REDEFINES`, `DATA_INTENT.OCCURS_DEPENDING_ON`, and
`UNSUPPORTED_EXPLANATION`. The initial closed validator vocabulary is `json-schema.v1`,
`annotated-ir-reference.v1`, `public-signature-preservation.v1`,
`deterministic-fallback.v1`, and `sanitized-persistence.v1`. Adding or changing selector or
validator semantics requires a versioned contract change.

The v1 catalog contains exactly these initial selector mappings:

| Prompt ID | Selector | Output schema | Fallback template |
| --- | --- | --- | --- |
| `cobol.domain.naming.v1` | `DOMAIN_NAMING` | `domain-naming.v1.schema.json` | `cobol.domain.naming.fallback.v1` |
| `cobol.goto.restructure.v1` | `CONTROL_FLOW_PLAN` | `control-flow-plan.v1.schema.json` | `cobol.goto.restructure.fallback.v1` |
| `cobol.redefines.intent.v1` | `DATA_INTENT.REDEFINES` | `data-intent.v1.schema.json` | `cobol.redefines.intent.fallback.v1` |
| `cobol.occurs-depending.intent.v1` | `DATA_INTENT.OCCURS_DEPENDING_ON` | `data-intent.v1.schema.json` | `cobol.occurs-depending.intent.fallback.v1` |
| `cobol.unsupported.explain.v1` | `UNSUPPORTED_EXPLANATION` | `unsupported-explanation.v1.schema.json` | `cobol.unsupported.explain.fallback.v1` |

Each fallback template is a versioned catalog resource that produces the deterministic fallback
shape defined in section 6 and cannot emit a model-authored proposal.

Runtime requests use temperature zero. This reduces variability but is not treated as a guarantee
of determinism; validated outputs and cache identities provide the reproducible boundary.

## 4. Provider-neutral runtime and Claude adapter

The core interface accepts a canonical enrichment request and returns a provider response without
exposing Anthropic-specific transport classes. The Anthropic adapter uses the Messages API and:

- reads the API credential only from `ANTHROPIC_API_KEY`;
- takes the Claude model from `renovatio.llm.anthropic.model`, with environment override
  `RENOVATIO_LLM_ANTHROPIC_MODEL`, rather than compiling a model name into cache logic;
- uses a 60-second request timeout;
- performs at most three total attempts;
- retries timeouts, HTTP 429, and HTTP 5xx responses only;
- uses exponential backoff with full jitter between retry attempts; and
- never retries authentication, authorization, malformed-request, schema, or validator failures.

Logs, exceptions, persisted artifacts, and Agora inputs must never contain the API key or
authorization headers. Tests use the offline fake and make no network calls.

Missing or blank model configuration or `ANTHROPIC_API_KEY` fails before attribution initialization
and before any network call with category `PROVIDER_CONFIGURATION_INVALID` and diagnostic code
`LLM_PROVIDER_CONFIGURATION_INVALID`.
Provider configuration validation is a preflight step: a governed cache-miss lifecycle begins only
after it succeeds, so a preflight rejection is not an unattributed cache miss.

The backoff base is 500 milliseconds, multiplier is 2, and maximum delay is 5 seconds. Full jitter
selects uniformly from zero through `min(5s, 500ms * 2^(retryNumber - 1))`. The random source and
scheduler are injected. Deterministic tests therefore assert the bound and selected delay without
sleeping; for three total attempts, the two retry-delay upper bounds are 500 and 1000 milliseconds.

## 5. Canonical identity and cache key

Canonical JSON uses the RFC 8785 implementation accepted by issue #124. The cache key is the
lowercase SHA-256 digest of an RFC 8785 canonical object containing exactly:

- `identityType: "renovatio.llm.cache-request"`;
- `identityVersion: 1`;
- the canonical annotated-IR enrichment input;
- `promptId` including its version;
- the immutable output-schema identifier and digest;
- the ordered validator identifiers and their versions;
- provider identifier;
- configured model identifier; and
- `runtimeContractVersion: "renovatio-llm.v1"`.

Consequently, any change to input, prompt version, schema, validators, provider, model, or runtime
contract invalidates the entry. Transport retry counts, timestamps, credentials, and Agora tool-run
IDs do not enter the key.

Cache entries live under a committed, module-owned resource directory partitioned by the first two
hexadecimal key characters and then the complete key. Each entry contains a strict versioned JSON
envelope with request metadata, validated result or fallback, hashes, and attribution metadata.
Raw credentials, authorization headers, unrestricted provider envelopes, and sensitive source
content are forbidden. A matching valid entry is returned without constructing or calling a
network provider.

On a cache miss, the runtime writes the validated and sanitized cache artifact to the working tree.
It never creates a Git commit itself. Promotion of that artifact into repository history is a
separate governed repository workflow, allowing tests and human review to inspect the diff first.
Envelope lifecycle uses two orthogonal fields. `resultDisposition` is exactly `MODEL_SUCCESS` or
`DETERMINISTIC_FALLBACK`; `promotionDisposition` is exactly `PENDING_PROMOTION`, `COMMITTED`, or
`INVALID_ATTRIBUTION`. Names such as `MISS_FALLBACK` are invalid because they conflate result and
promotion state.

The initial envelope is `PENDING_PROMOTION` and is not eligible to satisfy a cache lookup. It becomes
a valid cache hit only after the governed repository workflow validates it, changes its promotion
disposition to `COMMITTED`, recalculates the envelope content hash, and commits the finalized
envelope. The cache-miss
tool-run finalizes with the candidate's stable `repo://` URI, content hash, and pending disposition;
repository promotion records the finalized hash as a separate governed repository action, not
deferred attribution of the provider call. Cache lookup accepts only `COMMITTED` envelopes present
in repository history.

Repository presence is proven by a versioned `committed-cache-index.v1.json` generated during the
build exclusively from `git ls-tree HEAD`. Each index item binds cache key, repository path, and
envelope SHA-256. Runtime lookup requires an exact index entry and matching envelope digest; a file
that exists only in the working tree or packaged resources is ineligible. The build fails when the
index, tracked envelope, and `HEAD` tree disagree.

`envelopeHash` is lowercase SHA-256 over the RFC 8785 canonical UTF-8 envelope with only the
`envelopeHash` field itself omitted. Every other field, including promotion disposition,
participates. Promotion to `COMMITTED` therefore recalculates `envelopeHash` before index generation.

A cache hit requires exact equality of every declared cache-key component. Matching only the
canonical annotated IR and prompt version is insufficient when schema, validators, provider, model,
or runtime contract differ.

## 6. Validation and deterministic fallback

Successful provider output passes, in order:

1. strict JSON decoding and output-schema validation;
2. the prompt's ordered deterministic validators; and
3. annotated-IR semantic validation from the accepted issue #124 contract.

A provider failure, exhausted retry policy, malformed output, schema failure, or validator failure
produces a deterministic fallback containing:

- no model-authored semantic proposal;
- a stable failure category and diagnostic code;
- the deterministic transliteration/reference already available from the semantic core; and
- a reviewable manual action item rendered from the catalog fallback template.

Fallbacks are cached and attributed using the same request key and envelope as successful results,
but their `resultDisposition` is explicitly `DETERMINISTIC_FALLBACK` and records the non-sensitive
failure category.
They can never be interpreted as accepted model output or human acceptance.

The closed failure-category vocabulary is `PROVIDER_TIMEOUT`, `PROVIDER_RATE_LIMIT`,
`PROVIDER_SERVER_ERROR`, `PROVIDER_AUTHENTICATION`, `PROVIDER_REQUEST_REJECTED`,
`PROVIDER_CONFIGURATION_INVALID`,
`PROVIDER_UNAVAILABLE`, `OUTPUT_MALFORMED`, `OUTPUT_SCHEMA_INVALID`, `VALIDATOR_REJECTED`,
`SANITIZATION_REJECTED`, `ATTRIBUTION_INIT_FAILED`, and `ATTRIBUTION_FINALIZE_FAILED`. Each maps
one-to-one to a stable diagnostic code formed by prefixing the category with `LLM_`. New failure
semantics require a versioned contract change.

## 7. Agora attribution

Every cache miss executes through the governed Agora operation `llm-enrichment/enrich`, declared by
`.agora/tools/llm-enrichment/TOOL.md`. If attribution cannot be initialized, processing fails closed
before any provider call; no unattributed result or deferred reconciliation is permitted. This
single operation wraps the complete cache-miss lifecycle: it starts before the provider call with
the input and contract hashes, performs the provider/fallback path, and finalizes the same tool-run
with the output hash, cache disposition, stable failure category when applicable, and cache artifact
URI. If the run cannot be finalized, its output is not promoted as a valid cache entry. The tool-run
and cache envelope record:

- `promptId`, provider, model, and runtime contract version;
- input, output, schema, and cache-key SHA-256 values;
- result disposition (`MODEL_SUCCESS` or `DETERMINISTIC_FALLBACK`) and promotion disposition
  `PENDING_PROMOTION`;
- the `repo://` URI of the working-tree cache candidate; and
- the stable failure category when a fallback is produced.

The operation accepts hashes and reviewed identifiers, not raw credentials or authorization
headers. Cache artifacts use the URI contract
`repo://renovatio-llm/src/main/resources/llm-cache/<two-hex-prefix>/<cache-key>.json`.

Persistence is governed by a versioned allowlist schema. Metadata, stable identifiers, hashes, and
explicitly allowed typed result fields may be stored. Source-derived input or output is stored only
when every field is allowed by that schema and passes deterministic redaction. Credentials,
authorization headers, raw prompts, unrestricted IR/source text, provider response envelopes, and
fields classified as sensitive are forbidden. Diagnostics contain only a stable code, a redacted
message, and a configured maximum character count. If sanitization cannot be proven, the runtime
persists only non-sensitive fallback metadata and hashes; sensitive content is neither cached nor
placed in Agora records.

Cache hits do not call the provider. They remain auditable from the committed envelope and may emit
a local `HIT` event, but they do not masquerade as cache-miss tool-runs.

If the in-process attribution sink fails while the runtime can still act on the candidate, the
runtime moves the sanitized candidate out of the cache lookup tree into a local quarantine and marks
it `INVALID_ATTRIBUTION`. Quarantined artifacts are diagnostic-only: they cannot be loaded as cache
hits, registered as deliverable cache artifacts, or committed by the promotion workflow. The
quarantine obeys the same sanitization rules.

Agora persists the final tool result after the child process exits. A retrospective persistence
failure therefore cannot be reported back to that terminated process. In this case the sanitized
candidate remains `PENDING_PROMOTION`; that state is lookup-ineligible and promotion-ineligible until
reconciliation. Reconciliation reads the durable Agora `RUN.md` and `RESULT.md`, requires the same
tool-run to be `completed` with exit code zero, and binds its prompt, provider, model, input, output,
schema, cache-key, runtime-contract, artifact URI, and dispositions to the candidate. A missing,
failed, or mismatched record fails reconciliation and must be quarantined as `INVALID_ATTRIBUTION`
before any later promotion attempt. Successful reconciliation is repeated by the build against the
records present at Commit C, so a forged or subsequently missing attribution record blocks lookup.

The `agora-attribution` criterion is satisfied only by cache-miss tool-runs whose durable
reconciliation succeeds. Initialization failure prevents the provider call; in-process finalization
failure yields only quarantined `INVALID_ATTRIBUTION` diagnostic evidence; retrospective persistence
failure leaves only an ineligible pending candidate. None counts as successful attribution.

Cache promotion is executed by `project:agent` through the governed repository tool and accepted by
`project:owner`. Promotion uses three governed commits and keeps approval references out of the
technical index, eliminating self-reference:

1. Commit A validates the candidate, changes it to `COMMITTED`, recalculates its envelope hash, and
   commits the envelope.
2. `committed-cache-index.v1.json` is generated exclusively from Commit A's `HEAD` and committed as
   Commit B. Index entries contain only cache identity, repository path, and technical digests.
3. After Commit B exists, `project:owner` approves promotion and Agora registers `cache-promotion`
   evidence binding both commit SHAs, envelope hash, path, and index hash. `project:agent` then uses
   the governed repository tool to create Commit C, which persists only that approval and evidence.

During a build on Commit C or a descendant, the evidence and approval must match registered
`.agora/` records and the referenced Git objects; otherwise the build fails. The build emits a
digest-bound verified promotion manifest alongside the index. Runtime lookup does not re-query
Agora: it validates the envelope, index entry, and packaged verified manifest as one set. Lookup
eligibility begins only after Commit C and successful manifest generation.

## 8. Service naming and module boundaries

The existing provider-Cobol `LlmIntegrationService` only delegates to `NqlParserService`. It is
renamed exactly to `CobolNqlParsingService`, and its consumers/tests are updated. It must not gain
provider, prompt, credential, HTTP, or cache responsibilities.

`renovatio-llm` may depend on `renovatio-cobol-ir` and shared serialization utilities. The COBOL IR
module and OpenRewrite recipe module must not depend on HTTP clients, provider SDKs, credentials,
or network execution. Recipes consume only validated annotated models supplied through the existing
execution-context seam.

## 9. Lifecycle requirements

For the `spec-clarified` gate, an acceptance criterion is marked `specified` when this document
defines its normative contract, failure behavior, and at least one verifiable acceptance scenario.
That stage does not claim implementation or test completion.

The authoritative specification record is the `spec` artifact for this URI whose content digest
matches the current repository file. Earlier records for the same URI and different digests are
immutable specification history, not competing current specifications.

The required threat model and the versioned persistence-allowlist schema are planning deliverables.
They do not block clarification, but both must be registered and accepted before the work may enter
implementation.

## 10. Acceptance scenarios

- Loading a valid versioned prompt exposes every required catalog property; malformed or duplicate
  entries fail closed with stable diagnostics.
- A configured Claude adapter resolves `ANTHROPIC_API_KEY`, applies the declared timeout/retry
  policy, and can be replaced by an offline fake through the provider-neutral interface.
- Missing Claude model or credential fails locally with `LLM_PROVIDER_CONFIGURATION_INVALID` before
  attribution or network activity.
- Two identical canonical requests produce the same key; changing any declared identity component
  changes the key.
- A valid committed cache hit completes with zero provider calls.
- A cache miss produces a validated success or explicit deterministic fallback, a governed Agora
  tool-run, and a sanitized `PENDING_PROMOTION` cache candidate with matching hashes.
- A candidate becomes eligible for a cache hit only after the separate governed repository workflow
  promotes it to `COMMITTED`, recalculates its hash, and commits it.
- A build-derived index excludes working-tree-only entries and fails on any path or digest mismatch
  with `git ls-tree HEAD`.
- The build rejects promotion references or digests that do not match owner approval and Agora
  evidence; runtime accepts only the resulting digest-bound verified manifest.
- Promotion becomes lookup-eligible only after envelope Commit A, technical-index Commit B, and
  approval/evidence Commit C are present and mutually verified.
- An in-process attribution-finalization failure quarantines the candidate as
  `INVALID_ATTRIBUTION`; a retrospective Agora persistence failure leaves it `PENDING_PROMOTION`
  and ineligible until durable reconciliation succeeds.
- A failure to initialize `llm-enrichment/enrich` prevents the provider call and leaves no
  unattributed result.
- A cache miss writes a reviewable working-tree artifact; only the separate governed repository
  workflow commits it.
- Secrets and raw authorization data are absent from logs, cache entries, tests, and Agora records.
- `CobolNqlParsingService` replaces the misleading `LlmIntegrationService` name and remains limited
  to NQL parsing behavior.
