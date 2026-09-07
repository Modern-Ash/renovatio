# Implementation plan

1. Expand the equivalence DTO contract beyond read-only inventory to include fixtures, inputs, runs, comparisons, divergences, gate state, audit history, run requests, and triage requests.
2. Extend `WorkbenchEquivalenceService` with run, repeat, cancel, triage, report export, deterministic hashes, fixture discovery, gate evaluation, and audit events.
3. Add controller endpoints under `/workbench/equivalence/runs` with role checks and a domain-specific error handler.
4. Replace the Theia read-only equivalence area with an integrated Equivalence Lab surface that can start/repeat/cancel/export runs and triage divergences.
5. Add focused API tests plus UI contract coverage and run focused/broader verification.
