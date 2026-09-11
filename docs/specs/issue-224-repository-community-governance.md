# Issue #224 Spec: Repository Community Governance

## Objective

Prepare Renovatio for external review by removing accidental artifacts,
documenting contribution/security expectations, and defining repository evidence
retention boundaries.

## Scope

- Add community health files: `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`,
  `SECURITY.md`, `CHANGELOG.md`.
- Use DCO sign-off as the contribution model unless the owner later chooses a
  CLA.
- Remove tracked runtime/build artifacts from the current tree.
- Extend `.gitignore` to prevent recurrence.
- Document license decision, dependency license inventory, history scan findings,
  repo size measurements, and Agora retention policy.

## Human Decisions

- Repository license is MIT, approved by the owner on 2026-09-11. The
  repository includes the MIT license text at `LICENSE`.
- History rewrite is not performed in this issue. Any `filter-repo`, BFG, or
  force-push requires separate explicit approval.

## Acceptance Criteria Mapping

- `license`: owner-approved MIT license is present at the repository root.
- `community-files`: required community files exist and DCO is documented.
- `tracked-artifacts`: H2 database and `target_bad` are removed from the index
  and ignore rules prevent recurrence.
- `agora-retention`: retention policy is documented.
- `history-scan`: history scan findings are recorded with disposition.
- `dependency-license-scan`: preliminary inventory is recorded.
- `repo-size`: before/after measurements are recorded; no history rewrite is
  executed.
