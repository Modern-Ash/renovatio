# Implementation plan

1. Add a `renovatio-domain-model` module with immutable v1 records and enums for business nodes, relationships, evidence, provenance, confidence, and origin.
2. Add canonical JSON serialization/hash helpers and structural validation for references, duplicate IDs, project scope, and supported relationship kinds.
3. Add fixtures covering one program, multiple programs, and shared copybooks; prove deterministic equality and safe rejection of unsupported graphs.
4. Add module wiring/documentation without changing existing generation behavior; run focused tests, Maven compile, and diff-check.
