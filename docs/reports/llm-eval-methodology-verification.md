# LLM evaluation methodology verification

Status: passed.

Verification command:

```bash
mvn -pl renovatio-evals test
```

Result:

- Tests run: 11
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: success

Coverage:

- Passing critical suite emits a comparable report with acceptance, fallback, cost, latency, and cache-hit metrics.
- Hallucinated IR references fail closed.
- Outputs that attempt to promote final code fail the human-review boundary.
- Critical weighted-score regressions fail the prompt/model comparison gate.
- Report writing emits the same deterministic, baseline-comparable JSON document as direct evaluation.
- Suite identity fields are required before reports can be emitted.
- Declared IR references must resolve against the COBOL fixture, not only against a manual allowlist.
- Critical fixtures require baseline coverage whenever a baseline comparison is requested.
- Fallback aggregation is derived from `metrics.fallbackRate` in the output contract.
- Root-level final-code promotion fields are rejected, not only decision-level fields.
- Metric values must have valid types and ranges before aggregation.

Sample deterministic report: `docs/reports/llm-eval-methodology-sample-report.json`.
