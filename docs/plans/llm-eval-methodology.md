# LLM evaluation methodology implementation plan

1. Add a standalone `renovatio-evals` Maven module.
2. Define suite/output schema resources and versioned sample fixtures.
3. Implement an offline evaluator that checks schema bindings, known IR references, rubric thresholds, metrics, regression against baseline, and human-review boundaries.
4. Add tests for passing fixtures, hallucinated IR references, unsafe final-code promotion, and critical score regression.
5. Register the spec, implementation plan, verification report, and test evidence in Agora.
