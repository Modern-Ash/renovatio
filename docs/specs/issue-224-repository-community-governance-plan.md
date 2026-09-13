# Issue #224 Implementation Plan

## Steps

1. Inventory repository health files, tracked artifacts, history-sensitive paths
   and repository size.
2. Create Agora work for `architecture-convergence-2026/repository-community-governance`.
3. Add community files and DCO contribution policy.
4. Remove tracked `data/renovatio-db.mv.db` and
   `renovatio-provider-cobol/target_bad`.
5. Extend `.gitignore` for local databases, `target_bad`, dependency caches and
   Python environments.
6. Record license review, dependency license inventory, history scan,
   repository hygiene report and security report.
7. Verify with `git ls-files`, `git diff --check`, focused documentation checks,
   and repository size after cleanup.

## Explicit Non-Steps

- Do not choose a license without owner approval.
- Do not rewrite Git history.
- Do not delete required Agora release evidence from Git in this cycle.
