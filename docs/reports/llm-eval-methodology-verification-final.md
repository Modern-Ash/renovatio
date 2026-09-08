# LLM evaluation methodology final verification

Status: passed.

Verification command:

```bash
mvn -pl renovatio-evals test
```

Result:

- Tests run: 21
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
- Case prompt identity must match suite prompt identity to avoid report misattribution.
- Rubric scores must be numeric normalized values in the 0–1 range.
- Non-array `decisions` payloads fail closed before the decision loop runs.
- Cases without rubric definitions fail closed instead of receiving an implicit zero score.
- Baseline reports must match the evaluated suite, prompt, and model identity.
- Bare relative suite paths are normalized before resolving sibling fixtures and outputs.
- Output model identity must match the evaluated suite model.
- Promotion fields are rejected recursively, including nested metadata objects.
- Rubric configuration must provide positive weights and normalized minimum thresholds.
- COBOL statements such as `CONTINUE.` are not parsed as paragraph declarations.

Sample deterministic report: `docs/reports/llm-eval-methodology-sample-report.json`.
