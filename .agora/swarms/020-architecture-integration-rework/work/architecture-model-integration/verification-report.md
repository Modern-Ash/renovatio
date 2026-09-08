# Verification report

- `mvn -q -pl renovatio-architecture -am test`: PASS.
- `git diff --check`: PASS.
- Adapter test verifies preview graph consumption and kind mapping.
- Commit: `0e132be8` (full SHA recorded in Agora evidence).
- API endpoint `POST /api/projects/{projectId}/architecture-projection?style=...` accepts a versioned `DomainModel` and returns the projection for dashboard/workbench use (commit `1051d9c5`).
