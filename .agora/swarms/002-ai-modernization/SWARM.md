---
schema: "agora/swarm/v1"
id: "ai-modernization"
method: "spec-driven"
status: "completed"
branch: "agora/ai-modernization"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm ai-modernization

## Objective

Deliver a reproducible three-pass COBOL modernization pipeline: deterministic parsers and OpenRewrite recipes own core semantics; governed, content-addressed LLM enrichment operates only on IR; optional idiomatic polish produces review-only diffs with deterministic fallback.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
