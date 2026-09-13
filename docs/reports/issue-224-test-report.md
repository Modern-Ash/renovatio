# Issue #224 Test Report

Date: 2026-09-11

## Planned Verification

- `git ls-files data/renovatio-db.mv.db renovatio-provider-cobol/target_bad`
- `git diff --check`
- `git diff --check origin/main...HEAD`
- `git status --short`
- `mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=ProjectRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Repository size before/after commands recorded in hygiene report.

## Results

- `git ls-files data/renovatio-db.mv.db renovatio-provider-cobol/target_bad 'renovatio-provider-cobol/target_bad/**'`: success, no entries.
- `git diff --check`: success.
- `git diff --check origin/main...HEAD`: success after merge-conflict and review-comment remediation.
- `mvn -pl renovatio-api -am -Dexec.skip=true -Dtest=ProjectRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`: success; reactor built through `renovatio-api`, `ProjectRepositoryTest` ran 2 tests with 0 failures and 0 errors. `exec.skip=true` skips the API module's npm build because local `renovatio-ui/node_modules` is not installed in this worktree.
- Community files presence check: success.
- Repository size measured after cleanup: 4065 tracked files, working tree 31 MiB, Git object database still 111.09 MiB packed because history was not rewritten.
