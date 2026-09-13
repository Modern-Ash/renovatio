# Issue 231 Security Report

## Implemented Controls

- Added shared canonical workspace root policy.
- Restricted API project workspace creation to configured allowed roots.
- Restricted API Workbench list/read/write operations to canonical workspace paths.
- Restricted MCP file tools to process-root sandbox access.
- Rejected traversal, absolute paths outside the sandbox, and symlink boundaries.
- Preserved read-only behavior for legacy Workbench assets.

## Deferred Controls

- OIDC-backed remote identity.
- Full application-service authorization refactor away from `X-Role`.
- Automated SAST, dependency, and secret scans.
- Full remote-mode fail-closed smoke suite.

## Evidence

See `docs/reports/issue-231-test-report.md`.
