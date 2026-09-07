---
schema: "agora/clarifications/v1"
swarm: "renovatio-workbench"
work: "issue-176-theia-platform-spike"
created-at: "2026-09-06T01:39:32.904648Z"
last-run-input-sha256: "b658bedd9fa8eec3724133b2eca52df0f39742b517d85ae5ab31566a4188265d"
last-run-question-count: 5
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-09-06T01:40:53.814770Z"
---

# Clarifications for issue-176-theia-platform-spike

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| What distribution outcome is required from the spike? | The browser application is the primary deliverable. Desktop is a documented feasibility and packaging evaluation only; it is not a production distribution commitment. | project:owner | 2026-09-06T01:39:32.904648Z | 7127ec11d6d9c92a5195eed83e61cdc0059d2017ebf15049894e3a73b516c093 |
| When must Theia and Node versions become fixed? | The spike may compare candidates, but acceptance requires exact Theia and Node versions pinned in package manifests, the ADR, and the runbook, using a Node line supported by the selected Theia release. | project:owner | 2026-09-06T01:39:32.904648Z | 7127ec11d6d9c92a5195eed83e61cdc0059d2017ebf15049894e3a73b516c093 |
| Does this increment introduce a new authentication protocol? | No. It must preserve the existing backend authentication contract, expose the backend base URL through environment configuration, and never bundle secrets in frontend artifacts. | project:owner | 2026-09-06T01:39:32.904648Z | 7127ec11d6d9c92a5195eed83e61cdc0059d2017ebf15049894e3a73b516c093 |
| What extension compatibility boundary applies? | Validate Open VSX and an explicit allowlist of required extensions; document unsupported or proprietary VS Code Marketplace dependencies and do not assume universal VS Code extension compatibility. | project:owner | 2026-09-06T01:39:32.904648Z | 7127ec11d6d9c92a5195eed83e61cdc0059d2017ebf15049894e3a73b516c093 |
| Which cross-platform evidence is mandatory? | Linux CI must perform clean install, build, startup, and HTTP smoke checks. Browser operation on Linux and macOS must be recorded; macOS validation may be manual when no macOS CI runner exists, with the limitation stated in the verification report. | project:owner | 2026-09-06T01:39:32.904648Z | 7127ec11d6d9c92a5195eed83e61cdc0059d2017ebf15049894e3a73b516c093 |
| What must the implementation plan decide before the spike enters implementation? | It must select and pin the Theia and supported Node versions, package manager and lockfile strategy, prototype directory, CI workflow, smoke-test command, and removal or rollback path. | project:owner | 2026-09-06T01:40:53.814770Z | fc3043445b08fc8660962d69dafd5355d32d9e1456cf67ef3c944b65660a45b8 |
| May the prototype change Renovatio backend contracts to simplify integration? | No. It may use existing endpoints or a local proxy and define an authentication adapter seam, but backend API and domain behavior changes require separate governed work. | project:owner | 2026-09-06T01:40:53.814770Z | fc3043445b08fc8660962d69dafd5355d32d9e1456cf67ef3c944b65660a45b8 |
| What happens if the ADR rejects Theia? | The spike can still complete with evidence and an accepted rejection decision, but issue #177 remains blocked and must be revised against the replacement platform before resuming. | project:owner | 2026-09-06T01:40:53.814770Z | fc3043445b08fc8660962d69dafd5355d32d9e1456cf67ef3c944b65660a45b8 |
| What reproducibility boundary applies to dependencies? | All direct dependencies and runtime versions must be pinned through manifests and a committed lockfile; CI must install from that lockfile without silently accepting version drift. | project:owner | 2026-09-06T01:40:53.814770Z | fc3043445b08fc8660962d69dafd5355d32d9e1456cf67ef3c944b65660a45b8 |
| Does a screenshot satisfy prototype verification? | No. A screenshot supports the manual browser check only; clean build, startup and HTTP smoke evidence are mandatory, with commands, environment and content identity recorded. | project:owner | 2026-09-06T01:40:53.814770Z | fc3043445b08fc8660962d69dafd5355d32d9e1456cf67ef3c944b65660a45b8 |
