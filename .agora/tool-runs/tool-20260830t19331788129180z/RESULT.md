---
schema: "agora/tool-result/v1"
run: "tool-20260830t19331788129180z"
status: "completed"
exit-code: 0
result-kind: "repository-change"
---

# Tool result tool-20260830t19331788129180z

## Standard output

    [agora/issue-125-llm-runtime-catalog-cache 5f71959] feat(llm): add governed enrichment runtime and cache authority
     111 files changed, 4146 insertions(+), 108 deletions(-)
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/clarifications.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/consistency/consistency-20260830t18561788127011z.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/consistency/consistency-20260830t19221788128529z.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000001/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000002/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000003/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000004/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000005/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000006/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000007/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000008/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000009/EVIDENCE.md
     create mode 100644 .agora/swarms/002-ai-modernization/work/llm-runtime-catalog-cache/evidence/evidence-000010/EVIDENCE.md
     create mode 100644 .agora/tool-runs/tool-20260830t18151788124559z/RESULT.md
     create mode 100644 .agora/tool-runs/tool-20260830t18151788124559z/RUN.md
     create mode 100644 .agora/tool-runs/tool-20260830t19131788128010z/RESULT.md
     create mode 100644 .agora/tool-runs/tool-20260830t19131788128010z/RUN.md
     create mode 100644 .agora/tool-runs/tool-20260830t19141788128053z/RESULT.md
     create mode 100644 .agora/tool-runs/tool-20260830t19141788128053z/RUN.md
     create mode 100644 .agora/tools/llm-enrichment/TOOL.md
     create mode 100644 .agora/tools/llm-enrichment/operations/enrich.md
     create mode 100644 docs/plans/llm-runtime-catalog-cache-plan.md
     create mode 100644 docs/reports/llm-runtime-cache-authority-loader-20260830.md
     create mode 100644 docs/reports/llm-runtime-catalog-cache-full-verification-20260830.md
     create mode 100644 docs/reports/llm-runtime-catalog-cache-rework-report.md
     create mode 100644 docs/reports/llm-runtime-catalog-cache-test-report.md
     create mode 100644 docs/security/llm-runtime-threat-model.md
     create mode 100644 docs/specs/llm-runtime-catalog-cache.md
     create mode 100755 renovatio-llm/bin/renovatio-llm-enrich
     create mode 100644 renovatio-llm/pom.xml
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/CacheEnvelope.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/CacheIdentity.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/CacheKey.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/CommittedCacheArtifacts.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/CommittedCacheArtifactsLoader.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/CommittedCacheIndex.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/CommittedCacheIndexGenerator.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/ContentAddressedCache.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/DeterministicFallback.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/GitHeadRepositoryTree.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/PromotionDisposition.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/RepositoryTree.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/ResultDisposition.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cache/VerifiedPromotionManifest.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/cli/LlmEnrichmentCli.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/AgoraToolRunAttributionGateway.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/AttributionException.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/AttributionGateway.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/AttributionInput.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/AttributionResult.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/EnrichmentResult.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/GovernedEnrichmentService.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/enrichment/PersistenceSanitizer.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/CatalogFallbackFactory.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/OutputValidationException.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/PreparedEnrichment.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/PromptCatalog.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/PromptCatalogException.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/PromptCatalogLoader.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/PromptDefinition.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/PromptOutputValidator.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/PromptRuntime.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/prompt/StrictJsonSchemaValidator.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/AnthropicConfiguration.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/AnthropicHttpTransport.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/AnthropicLlmProvider.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/AnthropicTransport.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/LlmProvider.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/LlmRequest.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/LlmResponse.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/OfflineFakeProvider.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/ProviderException.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/ProviderFailure.java
     create mode 100644 renovatio-llm/src/main/java/org/shark/renovatio/llm/provider/RetryPolicy.java
     create mode 100644 renovatio-llm/src/main/resources/fallbacks/cobol.domain.naming.fallback.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/fallbacks/cobol.goto.restructure.fallback.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/fallbacks/cobol.occurs-depending.intent.fallback.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/fallbacks/cobol.redefines.intent.fallback.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/fallbacks/cobol.unsupported.explain.fallback.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/prompts/catalog-v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/prompts/cobol.domain.naming.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/prompts/cobol.goto.restructure.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/prompts/cobol.occurs-depending.intent.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/prompts/cobol.redefines.intent.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/prompts/cobol.unsupported.explain.v1.yaml
     create mode 100644 renovatio-llm/src/main/resources/schemas/control-flow-plan.v1.schema.json
     create mode 100644 renovatio-llm/src/main/resources/schemas/data-intent.v1.schema.json
     create mode 100644 renovatio-llm/src/main/resources/schemas/domain-naming.v1.schema.json
     create mode 100644 renovatio-llm/src/main/resources/schemas/persistence-allowlist.v1.json
     create mode 100644 renovatio-llm/src/main/resources/schemas/unsupported-explanation.v1.schema.json
     create mode 100644 renovatio-llm/src/test/java/org/shark/renovatio/llm/cache/CommittedCacheArtifactsLoaderTest.java
     create mode 100644 renovatio-llm/src/test/java/org/shark/renovatio/llm/cache/ContentAddressedCacheTest.java
     create mode 100644 renovatio-llm/src/test/java/org/shark/renovatio/llm/cli/LlmEnrichmentCliTest.java
     create mode 100644 renovatio-llm/src/test/java/org/shark/renovatio/llm/enrichment/GovernedEnrichmentServiceTest.java
     create mode 100644 renovatio-llm/src/test/java/org/shark/renovatio/llm/prompt/PromptCatalogLoaderTest.java
     create mode 100644 renovatio-llm/src/test/java/org/shark/renovatio/llm/provider/ProviderRuntimeTest.java
     create mode 100644 renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/CobolNqlParsingService.java
     delete mode 100644 renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/LlmIntegrationService.java
     create mode 100644 renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/service/CobolNqlParsingServiceTest.java
     delete mode 100644 renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/service/LlmIntegrationServiceTest.java

## Standard error

    (empty)
