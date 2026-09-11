# Issue #224 Test Report

Date: 2026-09-11

## Planned Verification

- `git ls-files data/renovatio-db.mv.db renovatio-provider-cobol/target_bad`
- `git diff --check`
- `git status --short`
- Repository size before/after commands recorded in hygiene report.

## Results

- `git ls-files data/renovatio-db.mv.db renovatio-provider-cobol/target_bad 'renovatio-provider-cobol/target_bad/**'`: success, no entries.
- `git diff --check`: success.
- Community files presence check: success.
- Repository size measured after cleanup: 4065 tracked files, working tree 31 MiB, Git object database still 111.09 MiB packed because history was not rewritten.
