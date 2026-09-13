# Issue #224 Security Report

Date: 2026-09-11

## History Scan

Command class:

- `git log --all --name-only` for sensitive paths and artifact classes.
- `git grep -n -I -E` over `git rev-list --all` for common credential patterns.

Findings:

- No private key blocks or cloud access-key patterns were found by the basic
  regex scan.
- Expected false positives:
  - GitHub Actions references to `${{ secrets.GITHUB_TOKEN }}`.
  - Documentation placeholder `${DB2_PASSWORD}`.
  - Source-code variables named `token`.
- Sensitive/artifact paths exist in history:
  - `data/renovatio-db.mv.db`
  - `renovatio-provider-cobol/target_bad/**`
  - historical `renovatio-ui/node_modules/**`

Disposition:

- Current-tree accidental artifacts are removed in this issue.
- Historical purge is deferred because it requires explicit approval for history
  rewrite and force-push.

## Dependency License Inventory

Preliminary package manifests:

- Maven reactor: root `pom.xml` plus 23 module POMs.
- npm: `renovatio-ui/package.json`, `renovatio-workbench/package.json`.
- Python: `renovatio-provider-python/pyproject.toml`.

Known direct ecosystem/license posture:

- Java dependencies are primarily Spring/Maven/OpenRewrite/JUnit ecosystem
  packages, commonly Apache-2.0/EPL/BSD/MIT family.
- npm and Theia dependencies require generated license inventory before final
  technical preview.
- Python direct dependencies are Jinja2, jsonschema and pytest test tooling,
  commonly permissive.

No incompatibility is accepted as resolved by this preliminary scan. Final
release should attach generated Maven/npm/Python license reports.
