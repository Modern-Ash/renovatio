# LLM attribution reconciliation verification

## Outcome

The cache-promotion verifier now reconciles every promoted envelope against the immutable Agora
tool-run records visible at Commit C. It requires a completed `llm-enrichment/enrich` invocation and
result with exit code zero, then compares prompt, provider, model, input, output, schema, cache key,
runtime contract, artifact URI, result disposition, and pending promotion disposition.

This closes the post-process persistence boundary without claiming that a terminated child process
can observe a later Agora write failure. Missing, failed, or mismatched durable attribution blocks
promotion; the candidate remains lookup-ineligible as `PENDING_PROMOTION` until reconciliation.
Observable in-process sink failures retain the existing immediate `INVALID_ATTRIBUTION` quarantine.

## Verification

Command:

```text
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/bin:/usr/bin:/bin mvn -pl renovatio-llm -am test
```

Result: exit code 0; 133 tests passed, 0 failed, 0 errors, 0 skipped across the focused reactor.
`GovernedPromotionVerifierTest` exercised the production verifier against the repository's actual
Commit A/B/C history and durable Agora run/result records.
