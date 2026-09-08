# Implementation plan

1. Add `SemanticDomainProjector` to `renovatio-domain-model` with deterministic mappings from program, data, I/O and side-effect nodes.
2. Preserve source path/span evidence and stable IDs; emit explicit relations and diagnostics for unsupported ambiguity.
3. Add fixtures for empty, data-bearing, database/file I/O and multi-program inputs; assert deterministic hashes and isolation.
4. Keep LLM integration as a validated adapter boundary only; do not invoke providers in the projector.
5. Run focused tests, Maven compile and diff-check, then register verification/review artifacts.
