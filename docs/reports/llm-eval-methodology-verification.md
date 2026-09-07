# LLM evaluation methodology verification

Status: passed.

Verification command:

```bash
mvn -pl renovatio-evals test
```

Result:

- Tests run: 5
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

Sample deterministic report: `docs/reports/llm-eval-methodology-sample-report.json`.
