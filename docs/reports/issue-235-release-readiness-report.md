---
issue: 235
epic: 221
agora_artifact: release-readiness-report
status: blocked
---

# Issue 235 Release Readiness Report

## Summary

Renovatio is not ready to publish `0.3.0-alpha.1` yet. The release cycle is
opened so the required gates, artifacts and blockers are explicit.

## Blocking Items

- AC-03 / issue #224 is still blocked by owner license approval.
- A release candidate SHA has not been frozen.
- Final build/test/equivalence/scanning evidence has not been generated for a
  candidate SHA.
- SBOM, checksums, provenance and signatures have not been generated.
- Spec Owner publication approval has not been granted.

## Dependency Status

The release depends on AC-01, AC-02, AC-03, AC-07, AC-09 and AC-10. Completion
must be rechecked immediately before candidate freeze because release readiness
depends on the current `main` state, not prior branch assumptions.

## Decision

Do not tag or publish. Continue only after dependency and license gates are
closed.

