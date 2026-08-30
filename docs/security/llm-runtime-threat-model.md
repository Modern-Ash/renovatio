# Threat model: Renovatio LLM runtime and cache

> Scope: issue #125 / `ai-modernization/llm-runtime-catalog-cache`

## Assets and trust boundaries

Protected assets are provider credentials, COBOL source-derived business data, annotated IR,
prompts, provider outputs, cache integrity, Agora attribution, and human approval. Trust boundaries
exist at environment configuration, the Anthropic HTTPS API, model output parsing, filesystem cache
and quarantine, Git history, build-time Agora verification, and runtime consumption of the verified
manifest.

## Threats and controls

| Threat | Control | Failure behavior |
| --- | --- | --- |
| Credential disclosure | Read only `ANTHROPIC_API_KEY`; forbid it and headers from models, logs, diagnostics, artifacts, and Agora inputs | Preflight or sanitization fails closed |
| Prompt injection through IR | Fixed versioned system prompts; typed selectors; strict output schema; deterministic validators | Discard output and produce deterministic fallback |
| Sensitive source persistence | Versioned field allowlist, deterministic redaction, bounded diagnostic codes/messages | Persist only hashes and non-sensitive fallback metadata |
| Cache poisoning | Complete canonical identity, envelope hash, committed index, Git-tree verification, owner-approved promotion manifest | Reject entry without provider fallback masquerading as a hit |
| Unattributed provider call | Initialize `llm-enrichment/enrich` before network; one operation wraps completion; reconcile durable run/result identity before promotion | No call when initialization fails; quarantine on observable in-process finalization failure; keep retrospective persistence failures pending and ineligible until reconciliation |
| Model nondeterminism | Temperature zero, content-addressed cache, schema/semantic validators | Conflict or invalid output becomes deterministic fallback |
| Retry amplification | Three total attempts; narrow retry classes; capped full jitter | Non-retryable errors fail immediately |
| Malicious/oversized output | HTTP/body limits, strict JSON decoding/schema, bounded diagnostics | Reject without logging raw response |
| Forged promotion | Four-commit A/B/C/D workflow, owner approval, Agora evidence, and a manifest-only Commit D whose content and ancestry are verified | Build fails and runtime rejects entry |
| Dependency boundary violation | Maven dependency tests/review; IR and recipe modules cannot depend on provider/HTTP code | Build/review blocks promotion |
| Quarantine reuse | Separate non-lookup path and `INVALID_ATTRIBUTION` disposition | Loader rejects quarantined artifacts |

## Persistence allowlist v1

Allowed fields are version identifiers, prompt/selector/validator IDs, provider/model identifiers,
stable failure categories, cache and content hashes, dispositions, bounded redacted diagnostics,
typed schema-approved result fields, governed URIs, Git SHAs, and Agora evidence/approval references.

Forbidden fields are credentials, tokens, authorization/cookie headers, raw prompts, unrestricted
COBOL/IR text, raw provider request/response envelopes, stack traces containing payloads, and any
field not explicitly declared by the versioned cache-envelope schema.

## Security acceptance

- Secret canaries never appear in logs, cache, quarantine, exceptions, or `.agora/` records.
- Unknown persistence fields and unredactable content fail closed.
- Working-tree-only, digest-mismatched, unapproved, or quarantined entries never produce hits.
- Provider calls are zero when attribution initialization or configuration preflight fails.
- A pending candidate cannot be promoted unless its durable Agora run/result is completed and all
  attribution identity fields match; failed reconciliation quarantines it before promotion.
- Runtime does not require credentials to exercise the offline fake or committed cache-hit path.
