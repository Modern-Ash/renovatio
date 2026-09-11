---
issue: 235
epic: 221
agora_work: architecture-convergence-2026/community-alpha-release
status: blocked-by-dependencies
---

# Issue 235 Implementation Plan

## Phase 0: Dependency Gate

- Confirm AC-01, AC-02, AC-03, AC-07, AC-09 and AC-10 are merged, closed and
  completed in Agora.
- Confirm the repository license decision from AC-03 is approved by the owner.
- Confirm whether AC-08 is included in the release scope or documented as
  experimental/offline.
- Freeze a release candidate SHA only after the above checks pass.

## Phase 1: Release Metadata

- Set `0.3.0-alpha.1` consistently across Maven modules, UI/workbench packages,
  docs and release notes.
- Generate changelog entries since tag `0.2.0`.
- Draft release notes with explicit limitations and support levels.

## Phase 2: Public Documentation

- Update README quick start for a clean clone.
- Align `ARCHITECTURE.md`, ADR index, security model, contribution guide,
  troubleshooting and examples with the candidate behavior.
- Add a capability matrix that maps each capability to evidence and support
  level.
- Run link and command checks against the public docs.

## Phase 3: Quality And Supply Chain

- Run full build, tests, characterization, equivalence and smoke tests on the
  candidate SHA.
- Run secret, dependency/license and generated-artifact scans.
- Generate SBOM, checksums, build provenance and signatures from the candidate
  artifacts.
- Verify the tag candidate in a fresh clone.

## Phase 4: Approval And Publication

- Publish the release-readiness report for Spec Owner review.
- Publish only after explicit approval.
- Create the tag and GitHub Release from the approved SHA.
- Record final evidence and close issue #235.

## Current State

This plan is prepared but implementation is blocked by dependencies, especially
AC-03 / issue #224 license approval.
