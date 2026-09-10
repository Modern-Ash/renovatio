# LLM evaluation methodology

Issue: GitHub #173.

Renovatio treats LLM output for domain and architecture modeling as advisory evidence, not as generated implementation. The evaluation package provides offline fixtures, versioned rubrics, schema checks, IR-reference validation, structural hallucination detection, safety gates, comparable baseline reports, and metric fields that can be attached to Agora work before prompt or model promotion.

## Scope

- Create `renovatio-evals` as a Maven module with no provider/network dependency.
- Version COBOL fixtures, evaluation suites, output samples, rubrics, and baselines.
- Validate every suggestion against known IR references for the fixture.
- Reject outputs that attempt to write final code or bypass human review.
- Emit deterministic comparable reports with acceptance, fallback, cost, latency, and cache-hit metrics.

## Non-goals

- No live LLM calls.
- No automatic prompt/model promotion.
- No generated target code from evaluation outputs.
