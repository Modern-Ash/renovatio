# Ollama COBOL Model Benchmark - 2026-09-16

## Scope

This report tracks local Ollama providers for COBOL discovery and reverse-engineering prompts.
The first implemented provider path is `--provider ollama`, backed by the native Ollama
`/api/chat` endpoint.

## Reproducible CLI Configuration

Use the same governed enrichment CLI arguments already used for Anthropic, changing only provider
and model:

```bash
RENOVATIO_LLM_OLLAMA_MODEL=sammcj/qwen2.5-cobol-coder-7b-instruct \
RENOVATIO_LLM_OLLAMA_ENDPOINT=http://localhost:11434/api/chat \
renovatio-llm/bin/renovatio-llm-enrich enrich \
  --provider ollama \
  --model sammcj/qwen2.5-cobol-coder-7b-instruct \
  --prompt-id <prompt-id> \
  --input-hash <sha256> \
  --cache-key <cache-key> \
  --schema-hash <sha256> \
  --runtime-contract-version renovatio-llm.v1
```

The provider enforces deterministic generation settings (`temperature: 0`, `stream: false`) and
parses JSON from either Ollama chat responses (`message.content`) or legacy generate responses
(`response`).

## Implemented Gate

| Criterion | Status | Evidence |
| --- | --- | --- |
| Ollama provider implemented | Passed | `OllamaLlmProvider`, `OllamaHttpTransport`, `OllamaConfiguration` |
| Fake/mock coverage | Passed | `ProviderRuntimeTest` covers retry, configuration, request shape, response parsing, malformed output |
| CLI selectable | Passed | `LlmEnrichmentCli` accepts `--provider ollama` and validates configured model |
| Local model comparison | Measured | `OllamaCobolDomainEntitiesModelTest` run against Qwen COBOL and XMAiNframe on 2026-09-16 |

## Result Template

| Model | Prompt/Input | Schema valid | Grounded evidence | Reverse-engineering quality | Latency | Cost | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `xmainframe-7b:q4_k_m` | `cobol.domain.entities.v1` Card-XREF facts | Pass | Pass | Pass: emits `CARD-XREF -> ACCOUNT` with `evidenceField=XREF-ACCT-ID` | ~9s test wall time | 0 local | Best current local model for this prompt |
| `sammcj/qwen2.5-cobol-coder-7b-instruct:q6_k` | `cobol.domain.entities.v1` Card-XREF facts | Pass | Pass | Fail: emits the relation reversed (`ACCOUNT -> CARD-XREF`) and uses target key `FD-ACCT-ID` as evidence | ~6s test wall time | 0 local | Useful for entity extraction, not reliable for relation direction/evidence without post-processing |

## Initial Recommendation

Ollama is now viable as an offline/local provider for experimentation and governed cache promotion.
For `cobol.domain.entities.v1`, prefer `xmainframe-7b:q4_k_m` over
`sammcj/qwen2.5-cobol-coder-7b-instruct:q6_k` when relation direction and evidence fields matter.
Do not enable Qwen as the default for entity-relation inference unless a deterministic
post-processor corrects MOVE-derived direction/evidence.
