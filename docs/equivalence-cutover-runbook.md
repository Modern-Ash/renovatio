# Equivalence cutover runbook

## Readiness gate

1. Run every approved COBOL/target fixture and archive its canonical JSON evidence.
2. Confirm the release gate has at least one report and no `REGRESSION` or `UNDETERMINED` result.
3. Review each `INTENTIONAL_CHANGE` with its approved business rationale before authorizing a target architecture.
4. Reconcile file outputs, record state, business-rule invariants, return codes, errors, transactions and temporary datasets for the selected canary cohort.

## Shadow and canary execution

Shadow mode is read-only: invoke the target with replayed historical inputs, retain its outputs separately,
and make no target-owned production writes. Begin with a bounded, representative canary cohort; stop expansion
on any release-gate blocker, unexpected return code, or unreconciled transaction delta.

## Rollback

1. Freeze target routing and new target writes.
2. Route the affected cohort back to the established COBOL flow.
3. Preserve target evidence, request/response correlation IDs and reconciliation deltas; do not delete them.
4. Reconcile the bounded interval and repair only through an approved data-recovery procedure.
5. Record the rollback outcome and require fresh equivalent evidence before a new canary.

This repository provides the deterministic local gate and fixture evidence. A production shadow/canary run and
rollback drill require an approved operational environment and must be recorded as separate deployment evidence.
