# LLM runtime full-reactor verification

## Revision

Tested commit: `5b52893f` (`fix(llm): close governed runtime verification gaps`).

## Command and result

```text
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/bin:/usr/bin:/bin mvn test
```

Result: exit code 0; all 10 reactor projects built successfully. The reactor executed 250 tests
with 250 passing and no failures or errors. The MCP integration class reported zero executed tests
because no external server was available; its 22 deterministic module tests passed. No live LLM
provider request was made.

The run includes committed-cache promotion-history and durable-attribution reconciliation in
`GovernedPromotionVerifierTest`, annotated-IR semantic rejection/fallback behavior, lazy provider
construction on cache hits, and the existing Renovatio regression suite.
