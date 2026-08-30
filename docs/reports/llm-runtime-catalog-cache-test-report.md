# LLM runtime, catalog, and cache verification report

Date: 2026-08-30

## Result

The Java 17 reactor completed successfully with 236 tests, zero failures, zero errors, and zero
skips reported by the module summaries.

Command:

```text
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn test
```

The focused dependency reactor for `renovatio-llm` also completed successfully with 119 tests.

## Acceptance coverage

- `PromptCatalogLoaderTest` verifies valid catalog loading and fail-closed rejection of malformed,
  unknown, or incomplete versioned prompt definitions.
- `ProviderRuntimeTest` verifies provider-neutral execution, Anthropic request policy, bounded retry,
  timeout configuration, and the offline fake.
- `ContentAddressedCacheTest` verifies complete cache identity, committed-only lookup, pending
  candidates, deterministic fallback, and quarantine behavior.
- `GovernedEnrichmentServiceTest` verifies attribution starts before provider access, successful
  finalization, provider fallback, finalization quarantine, and secret/unknown-field rejection.
- `CobolNqlParsingServiceTest` verifies that NQL parsing remains functional under its accurate
  service name.

## Additional checks

- `agora tool list` discovers `llm-enrichment/enrich`.
- `agora validate` returns `ok: true` after refreshing the project pack composition lock.
- No live provider call was made; provider behavior is covered offline, and credentials remain
  environment-only.

## Known non-blocking observations

The reactor retains pre-existing compiler warnings and the MCP integration probe reports zero
executed integration tests when its local servers are absent. The remaining 22 MCP module tests and
all other module tests pass; neither observation is introduced by this change.
