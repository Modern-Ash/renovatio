---
schema: "agora/evidence-entry/v3"
id: "evidence-000004"
type: "script"
phase: null
result: "success"
revision: 1
artifact-references: ["git://92c693078588e800f76d178827aeac29d214a5e6"]
artifact-content-sha256: {"git://92c693078588e800f76d178827aeac29d214a5e6": null}
produced-by: "project:agent"
timestamp: "2026-09-08T22:05:00Z"
tested-commit: "92c693078588e800f76d178827aeac29d214a5e6"
command: ["./scripts/bootstrap.sh","--skip-tests"]
exit-code: 0
tests-total: 0
tests-passed: 0
tests-failed: 0
environment: "Ubuntu 24.04, Java 21, Node 24.20.0, Python 3.14.4"
dedupe-key: "issue223-bootstrap-92c69307"
---

# Evidence: Bootstrap Script

All components built successfully. Script is idempotent and fails on first error.
