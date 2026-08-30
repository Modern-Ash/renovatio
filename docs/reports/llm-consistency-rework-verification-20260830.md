# LLM consistency rework verification

## Rework completed

- Provider configuration is resolved after cache lookup but before attribution initialization.
- Cache hits remain provider- and attribution-free; outer Agora invocation is classified as
  dispatch rather than successful miss attribution.
- Fallback templates now have a strict four-field versioned contract validated at startup.
- The no-op `deterministic-fallback.v1` model-output validator was removed.
- Promotion documentation now includes generated-manifest Commit D.

## Verification

The Java 17 focused dependency reactor completed successfully with 135 tests: 135 passed, zero
failed, zero errors. New negative tests cover invalid fallback version, type, action, and unknown
fields, plus provider-configuration failure before any attribution call.
