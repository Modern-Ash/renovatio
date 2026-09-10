# LLM runtime, catalog, and cache rework verification report

Date: 2026-08-30

## Result

The focused Java 17 dependency reactor for `renovatio-llm` completed successfully after rework with
128 tests. This includes the executable adapter, strict schema validation, verified committed-cache
lookup, and attribution-envelope metadata.

## Acceptance coverage

- `PromptCatalogLoaderTest` verifies strict versioned prompt definitions.
- `ProviderRuntimeTest` verifies provider-neutral execution and Anthropic policy.
- `ContentAddressedCacheTest` verifies committed-only lookup and promotion proofs.
- `GovernedEnrichmentServiceTest` verifies fail-closed attribution and sanitized fallback.
- `LlmEnrichmentCliTest` verifies the production executable boundary against a running Agora record
  and rejects identity mismatches before persistence.

## Governed executable check

`agora tool show --tool llm-enrichment` resolves the repository executable. Governed tool-run
`tool-20260830t19141788128053z` completed through `agora tool invoke --launch` with the deterministic
offline provider. Its result records the input, output, schema, cache and envelope hashes; the cache
candidate records the same tool-run reference and remains `PENDING_PROMOTION`.

No network provider call was made and credentials remain environment-only.

The preceding probe `tool-20260830t19131788128010z` failed because its launcher attempted to use a
Maven plugin under the read-only runtime sandbox. The launcher was replaced with direct Java
execution over the already-built classpath before the successful governed run.
