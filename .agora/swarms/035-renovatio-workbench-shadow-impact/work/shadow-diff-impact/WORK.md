---
schema: "agora/work/v1"
id: "shadow-diff-impact"
swarm: "renovatio-workbench-shadow-impact"
title: "Theia 5 \u00b7 Shadow, diff e impact analysis"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"output-root-diff":"Existing generated targets are scanned relative to ProjectEntity.javaOutputPath so planned and existing manifests compare in the same namespace.","truthful-traceability":"Unmatched artifacts do not inherit unrelated domain evidence; deterministic links require matched domain evidence.","stale-response-guard":"Shadow impact responses are discarded when a project switch makes them stale.","ui-export-contract":"The Theia Shadow area exposes loading, empty, permission, error, refresh and JSON export states without mutating generation flows.","verification-evidence":"Backend, UI, smoke and characterization checks pass, and review threads are resolved before merge."}
satisfied-criteria: ["output-root-diff","truthful-traceability","stale-response-guard","ui-export-contract","verification-evidence"]
criterion-statuses: {"output-root-diff":["specified","planned","implemented","verified","accepted"],"truthful-traceability":["specified","planned","implemented","verified","accepted"],"stale-response-guard":["specified","planned","implemented","verified","accepted"],"ui-export-contract":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","implementation","verification","review-resolution"]
child-work-refs: []
budget-limits: null
---

# Theia 5 · Shadow, diff e impact analysis

## Description

GitHub #181: expose a read-only Shadow workbench area that computes planned vs existing generated artifacts from the configured output root, links impacts only through deterministic domain evidence, handles stale project switches safely, exports a canonical report, and preserves review/CI gates.

## Acceptance criteria

- [x] **output-root-diff:** Existing generated targets are scanned relative to ProjectEntity.javaOutputPath so planned and existing manifests compare in the same namespace.; stages: specified, planned, implemented, verified, accepted
- [x] **truthful-traceability:** Unmatched artifacts do not inherit unrelated domain evidence; deterministic links require matched domain evidence.; stages: specified, planned, implemented, verified, accepted
- [x] **stale-response-guard:** Shadow impact responses are discarded when a project switch makes them stale.; stages: specified, planned, implemented, verified, accepted
- [x] **ui-export-contract:** The Theia Shadow area exposes loading, empty, permission, error, refresh and JSON export states without mutating generation flows.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** Backend, UI, smoke and characterization checks pass, and review threads are resolved before merge.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- implementation
- verification
- review-resolution
