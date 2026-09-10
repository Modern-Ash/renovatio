---
schema: "agora/work/v1"
id: "hardening-distribution-adoption"
swarm: "renovatio-workbench-hardening-adoption"
title: "Theia 9 \u00b7 Hardening, distribuci\u00f3n y adopci\u00f3n"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"installation-doc":" Installation from a clean clone is documented and includes build, tests, audit, performance, pilot and smoke commands.","ci-gates":" CI blocks critical regressions across extension, API, UI, E2E smoke, Docker build, hardening, performance and production audit gates.","dashboard-continuity":" The previous dashboard remains operational through the RENOVATIO_DASHBOARD_URL boundary and is not replaced by Theia.","performance-logs":" Opening time, smoke startup, RSS and bundle thresholds are defined with operational log and metric guidance.","security-audit":" Workspace isolation, CSP, telemetry opt-in, command allowlist and MCP allowlist are documented and automatically audited.","compatibility-distribution":" Theia, Node, npm, Open VSX and desktop compatibility/release risks are versioned.","llm-handoff-pilot":" A guide lets another LLM continue the work and the COBOL demo pilot fixture set is hash-validated."}
satisfied-criteria: ["installation-doc","ci-gates","dashboard-continuity","performance-logs","security-audit","compatibility-distribution","llm-handoff-pilot"]
criterion-statuses: {"installation-doc":["specified","planned","implemented","verified","accepted"],"ci-gates":["specified","planned","implemented","verified","accepted"],"dashboard-continuity":["specified","planned","implemented","verified","accepted"],"performance-logs":["specified","planned","implemented","verified","accepted"],"security-audit":["specified","planned","implemented","verified","accepted"],"compatibility-distribution":["specified","planned","implemented","verified","accepted"],"llm-handoff-pilot":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","implementation","verification","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 9 · Hardening, distribución y adopción

## Description

Prepare Renovatio Workbench for continuous web use without breaking the existing dashboard or governed backend contracts.

## Acceptance criteria

- [x] **installation-doc:**  Installation from a clean clone is documented and includes build, tests, audit, performance, pilot and smoke commands.; stages: specified, planned, implemented, verified, accepted
- [x] **ci-gates:**  CI blocks critical regressions across extension, API, UI, E2E smoke, Docker build, hardening, performance and production audit gates.; stages: specified, planned, implemented, verified, accepted
- [x] **dashboard-continuity:**  The previous dashboard remains operational through the RENOVATIO_DASHBOARD_URL boundary and is not replaced by Theia.; stages: specified, planned, implemented, verified, accepted
- [x] **performance-logs:**  Opening time, smoke startup, RSS and bundle thresholds are defined with operational log and metric guidance.; stages: specified, planned, implemented, verified, accepted
- [x] **security-audit:**  Workspace isolation, CSP, telemetry opt-in, command allowlist and MCP allowlist are documented and automatically audited.; stages: specified, planned, implemented, verified, accepted
- [x] **compatibility-distribution:**  Theia, Node, npm, Open VSX and desktop compatibility/release risks are versioned.; stages: specified, planned, implemented, verified, accepted
- [x] **llm-handoff-pilot:**  A guide lets another LLM continue the work and the COBOL demo pilot fixture set is hash-validated.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- implementation
- verification
- review-report
