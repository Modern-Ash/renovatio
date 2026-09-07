# Spec — deterministic equivalence harness

The first governed increment of issue #174 compares COBOL and generated targets through a shared
runner boundary. Each fixture records a stable input identifier, source and target SHA-256 output
hashes, business invariants, classification, reason and whether the result blocks release.

Comparison modes are byte/hash exact, record exact, numeric-equivalent and named business-rule
equivalence. Operational replay, shadow mode, canary routing and production cutover remain deferred
until an approved runtime exists; this increment supplies their local release boundary and rollback
runbook.

Acceptance is denied for missing hashes, invariant deltas without an approved intentional-change flag,
regressions, or undetermined results.
