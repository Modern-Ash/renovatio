# Issue #224 Repository Hygiene Report

Date: 2026-09-11

## Before Inventory

- Tracked files: 4181.
- Working tree size: 33 MiB.
- Agora files: 2398.
- Agora size: 16 MiB.
- Git object database: 111.09 MiB packed, 8.78 MiB loose.

Tracked accidental artifacts found:

- `data/renovatio-db.mv.db`
- `renovatio-provider-cobol/target_bad/**`

History-sensitive paths found:

- `data/renovatio-db.mv.db`
- `renovatio-provider-cobol/target_bad/**`
- historical `renovatio-ui/node_modules/**`

## Changes

- Removed `data/renovatio-db.mv.db` from the current tree.
- Removed tracked `renovatio-provider-cobol/target_bad/**` output from the
  current tree.
- Added ignore rules for local databases, `target_bad`, dependency caches and
  Python virtual environments.
- Added community health files and DCO contribution policy.

## After Inventory

- Tracked files: 4065.
- Working tree size: 31 MiB.
- Removed tracked accidental artifact paths verified by:
  `git ls-files data/renovatio-db.mv.db renovatio-provider-cobol/target_bad 'renovatio-provider-cobol/target_bad/**'`
  returning no entries.
- Git object database remains 111.09 MiB packed because no history rewrite was
  performed.

## Agora Retention Policy

Keep in Git:

- Swarm/work indexes and current state records.
- Specs, implementation plans, approvals, decisions and release-critical
  evidence summaries.
- Small evidence records needed to prove release gates.

Do not keep in Git by default:

- Large command logs, generated archives, screenshots, local databases, build
  directories, dependency caches or runtime traces.
- Voluminous historical execution output unless it is explicitly required for a
  release audit.

Externalize or archive:

- Large evidence bundles should be attached to GitHub releases or another
  immutable artifact store, referenced from the compact Agora evidence record.

## History Rewrite

No history rewrite was performed. Removing the historical database,
`target_bad`, or `node_modules` blobs from prior commits requires a separate
explicit approval for `git filter-repo`, BFG, and force-push coordination.
