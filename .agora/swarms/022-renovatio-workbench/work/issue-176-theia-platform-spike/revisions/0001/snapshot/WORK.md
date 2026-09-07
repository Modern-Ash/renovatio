---
schema: "agora/work/v1"
id: "issue-176-theia-platform-spike"
swarm: "renovatio-workbench"
title: "Issue #176 \u00b7 Theia 0 \u00b7 Spike de plataforma y distribuci\u00f3n"
state: "completed"
revision: 1
operational-status: "active"
status-reason: "El usuario confirm\u00f3 la validaci\u00f3n manual del Workbench en http://127.0.0.1:3001 con captura y autoriz\u00f3 excluir macOS del alcance. Tambi\u00e9n autoriz\u00f3 una excepci\u00f3n limitada al advisory cr\u00edtico de desarrollo de Theia CLI/decompress; no aplica a producci\u00f3n o release. Los riesgos moderados del runtime se mantienen limitados al spike local y documentados para revisi\u00f3n futura."
status-by: "project:agent"
status-at: "2026-09-06T12:09:22.806512Z"
acceptance-criteria: {"prototype-build":"A minimal Theia application with a local workspace builds cleanly in CI and starts successfully.","renovatio-widget":"A Renovatio widget is visible and a registered command opens its custom view.","platform-distribution":"Browser execution is confirmed on Linux and macOS, with desktop packaging evaluated as a secondary distribution.","extension-compatibility":"Open VSX, extension compatibility, pinned Node version, file tree integration, menu and command behavior are validated and documented.","environment-security":"Authentication, backend URL and per-environment configuration are defined; licensing, telemetry, CSP and extension limits are documented.","adr-decision":"An ADR compares Theia, Code-OSS, OpenVSCode Server and Monaco, justifies the platform decision and pins the selected Theia version.","non-production-impact":"The spike does not modify or replace the current production flow or dashboard.","verification-evidence":"A clean build, HTTP smoke test, prototype capture and technical review are recorded as successful evidence."}
satisfied-criteria: ["prototype-build","renovatio-widget","platform-distribution","extension-compatibility","environment-security","adr-decision","non-production-impact","verification-evidence"]
criterion-statuses: {"prototype-build":["specified","planned","implemented","verified","accepted"],"renovatio-widget":["specified","planned","implemented","verified","accepted"],"platform-distribution":["specified","planned","implemented","verified","accepted"],"extension-compatibility":["specified","planned","implemented","verified","accepted"],"environment-security":["specified","planned","implemented","verified","accepted"],"adr-decision":["specified","planned","implemented","verified","accepted"],"non-production-impact":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","prototype","architecture-decision-record","runbook","compatibility-risk-matrix","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Issue #176 · Theia 0 · Spike de plataforma y distribución

## Description

Validate Eclipse Theia as the web-first platform for Renovatio before migrating production screens. Build a minimal local-workspace prototype, evaluate browser and desktop distribution, extensions, configuration, licensing, telemetry and security constraints, without changing the production flow.

## Acceptance criteria

- [x] **prototype-build:** A minimal Theia application with a local workspace builds cleanly in CI and starts successfully.; stages: specified, planned, implemented, verified, accepted
- [x] **renovatio-widget:** A Renovatio widget is visible and a registered command opens its custom view.; stages: specified, planned, implemented, verified, accepted
- [x] **platform-distribution:** Browser execution is confirmed on Linux and macOS, with desktop packaging evaluated as a secondary distribution.; stages: specified, planned, implemented, verified, accepted
- [x] **extension-compatibility:** Open VSX, extension compatibility, pinned Node version, file tree integration, menu and command behavior are validated and documented.; stages: specified, planned, implemented, verified, accepted
- [x] **environment-security:** Authentication, backend URL and per-environment configuration are defined; licensing, telemetry, CSP and extension limits are documented.; stages: specified, planned, implemented, verified, accepted
- [x] **adr-decision:** An ADR compares Theia, Code-OSS, OpenVSCode Server and Monaco, justifies the platform decision and pins the selected Theia version.; stages: specified, planned, implemented, verified, accepted
- [x] **non-production-impact:** The spike does not modify or replace the current production flow or dashboard.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** A clean build, HTTP smoke test, prototype capture and technical review are recorded as successful evidence.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- prototype
- architecture-decision-record
- runbook
- compatibility-risk-matrix
- verification-report
- review-report
