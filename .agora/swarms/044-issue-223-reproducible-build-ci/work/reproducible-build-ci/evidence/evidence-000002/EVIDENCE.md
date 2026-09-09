---
schema: "agora/evidence-entry/v3"
id: "evidence-000002"
type: "build"
phase: null
result: "success"
revision: 1
artifact-references: ["git://92c693078588e800f76d178827aeac29d214a5e6"]
artifact-content-sha256: {"git://92c693078588e800f76d178827aeac29d214a5e6": null}
produced-by: "project:agent"
timestamp: "2026-09-08T22:06:00Z"
tested-commit: "92c693078588e800f76d178827aeac29d214a5e6"
command: ["npm","ci","&&","npm","test","&&","npm","run","build"]
exit-code: 0
tests-total: 49
tests-passed: 49
tests-failed: 0
environment: "Ubuntu 24.04, Node 24.20.0, npm 11.19.0"
dedupe-key: "issue223-node-92c69307"
---

# Evidence: Node Build

UI: 28 tests passed. Workbench: 21 tests passed. Both built successfully.
