---
schema: "agora/work/v1"
id: "f0-decision-cartography"
swarm: "decision-engine"
title: "F0 \u00b7 Decision-model cartography (issue #145)"
state: "completed"
revision: 2
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"catalog":"docs/specs/decision-model-cartography.md lists >=15 decision points with category, typical location, current option, alternative options, expected heuristic confidence, LLM-delegation recommendation, characterization-verifiable flag","categories":"Every category (NUMERIC, CONTROL_FLOW, DATA_SHAPE, PERSISTENCE, NAMING, ARCHITECTURE) has >=1 real example cited from the fixtures","coupling-map":"The document identifies every input consumed by JavaGenerationService and MigrationPlanService","f1-recommendation":"The document ends with a recommended F1 scope cut based on the findings"}
satisfied-criteria: ["catalog","categories","coupling-map","f1-recommendation"]
criterion-statuses: {"catalog":["specified","planned","implemented","verified","accepted"],"categories":["specified","planned","implemented","verified","accepted"],"coupling-map":["specified","planned","implemented","verified","accepted"],"f1-recommendation":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["docs/specs/decision-model-cartography.md"]
child-work-refs: []
budget-limits: null
---

# F0 · Decision-model cartography (issue #145)

## Description

Spike: inventory implicit decision points in the current COBOL->Java generation path, map engine coupling, assess semantic-IR reuse and renovatio-llm reuse, prioritize by fixture frequency. No production code. GitHub issue #145, Epic #152.

## Acceptance criteria

- [x] **catalog:** docs/specs/decision-model-cartography.md lists >=15 decision points with category, typical location, current option, alternative options, expected heuristic confidence, LLM-delegation recommendation, characterization-verifiable flag; stages: specified, planned, implemented, verified, accepted
- [x] **categories:** Every category (NUMERIC, CONTROL_FLOW, DATA_SHAPE, PERSISTENCE, NAMING, ARCHITECTURE) has >=1 real example cited from the fixtures; stages: specified, planned, implemented, verified, accepted
- [x] **coupling-map:** The document identifies every input consumed by JavaGenerationService and MigrationPlanService; stages: specified, planned, implemented, verified, accepted
- [x] **f1-recommendation:** The document ends with a recommended F1 scope cut based on the findings; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- docs/specs/decision-model-cartography.md
