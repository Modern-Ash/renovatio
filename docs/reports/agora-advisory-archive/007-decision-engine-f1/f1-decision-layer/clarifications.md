---
schema: "agora/clarifications/v1"
swarm: "decision-engine-f1"
work: "f1-decision-layer"
created-at: "2026-09-01T10:56:29.093952Z"
last-run-input-sha256: "195dfcd06743f5f86a626650808372e2f1e94c72ce1c72df0d2aa1685bc730e2"
last-run-question-count: 0
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-09-01T11:11:28.398169Z"
---

# Clarifications for f1-decision-layer

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| What registered `spec` artifact will govern this work, and where does it define the complete issue #146 contract required before the spec-owner may transition the work to `clarified`? |  | project:owner | 2026-09-01T10:56:29.093952Z | 8a8c6728755cfa80b225c8a9cde58456afb64137c21e6de119d5a5a28cea02c2 |
| What are the exact HTTP methods, paths, request/response schemas, filtering rules, validation rules, and 400-versus-422 conditions for each of the five profile and decision endpoints? |  | project:owner | 2026-09-01T10:56:29.093952Z | 8a8c6728755cfa80b225c8a9cde58456afb64137c21e6de119d5a5a28cea02c2 |
| Which F0 cartography revision, current Java output fixtures, characterization harness, MCP regression checks, and byte-comparison procedure constitute the authoritative compatibility baseline? |  | project:owner | 2026-09-01T10:56:29.093952Z | 8a8c6728755cfa80b225c8a9cde58456afb64137c21e6de119d5a5a28cea02c2 |
| What are the complete DecisionPoint model, allowed statuses and transitions, semantic-coordinate ID algorithm, persistence behavior, resolver precedence, option-validation rules, and threshold bulk-confirm semantics? |  | project:owner | 2026-09-01T10:56:29.093952Z | 8a8c6728755cfa80b225c8a9cde58456afb64137c21e6de119d5a5a28cea02c2 |
| What observable behavior and test assertions define acceptance for LLM failure telemetry, non-blocking deterministic fallback, and the Target/Decisions wizard interactions, including filtering, rationale, confirmation, editing, and bulk confirmation? |  | project:owner | 2026-09-01T10:56:29.093952Z | 8a8c6728755cfa80b225c8a9cde58456afb64137c21e6de119d5a5a28cea02c2 |
| What concurrency token must PUT /api/projects/{id}/profile supply—request-body revision, If-Match, or another mechanism—and does an identical PUT preserve the current revision rather than create a new one? |  | project:owner | 2026-09-01T11:06:16.118212Z | 5092681535097ddc72b9e73dd406f90ef2e96c147c7cde71a31a3f1ae305edfc |
| What canonical location (programId, nodeKind, and nodeId) is assigned to each of the seven F0 decision keys, and how are multiple node-level values represented without collisions in resolvedDecisions, which is currently keyed only by decisionKey? |  | project:owner | 2026-09-01T11:06:16.118212Z | 5092681535097ddc72b9e73dd406f90ef2e96c147c7cde71a31a3f1ae305edfc |
| Which decision keys override first-class profile fields, and what exact mapping implements the stated defaults < profile < confirmed/overridden decisions precedence for canonical hashing? |  | project:owner | 2026-09-01T11:06:16.118212Z | 5092681535097ddc72b9e73dd406f90ef2e96c147c7cde71a31a3f1ae305edfc |
| Which versioned prompt handles eligible ARCHITECTURE decisions, given that java.framework-coupling is an F1 decision but the five listed prompt IDs omit an architecture prompt? |  | project:owner | 2026-09-01T11:06:16.118212Z | 5092681535097ddc72b9e73dd406f90ef2e96c147c7cde71a31a3f1ae305edfc |
| What legacy javaArchitecture values are recognized, how does each map to architecture.style, and at what lifecycle event does the idempotent legacy-column importer run? |  | project:owner | 2026-09-01T11:06:16.118212Z | 5092681535097ddc72b9e73dd406f90ef2e96c147c7cde71a31a3f1ae305edfc |
| Has `project:owner`, acting as Spec Owner, registered `repo://docs/specs/f1-decision-layer.md` as the required `spec` artifact and explicitly confirmed that every acceptance criterion is sufficiently specified for the `spec-clarified` gate? |  | project:owner | 2026-09-01T11:09:29.365757Z | 50b0ee91a5856c5d7fc02cf778f3ff53ffa573b8962c51b01ddaba2ac679d299 |
| What is the complete closed vocabulary for `llmFailureCategory`, and which provider, attribution, timeout, malformed-JSON, schema, option, sanitizer, and cache failures map to each value? |  | project:owner | 2026-09-01T11:09:29.365757Z | 50b0ee91a5856c5d7fc02cf778f3ff53ffa573b8962c51b01ddaba2ac679d299 |
| What exact heuristic confidence value should be persisted for each of the seven F1 decisions, including after deterministic creation or invalidation during re-analysis? |  | project:owner | 2026-09-01T11:09:29.365757Z | 50b0ee91a5856c5d7fc02cf778f3ff53ffa573b8962c51b01ddaba2ac679d299 |
| How should `PATCH` behave when the submitted option equals the current choice but the record already has `source=USER` or status `CONFIRMED`/`OVERRIDDEN`; specifically, what are the resulting status, source, confidence, and rationale? |  | project:owner | 2026-09-01T11:09:29.365757Z | 50b0ee91a5856c5d7fc02cf778f3ff53ffa573b8962c51b01ddaba2ac679d299 |
| Which cross-field profile combinations are invalid and must return `422`, beyond the field-level schema constraints and the separately defined `TARGET_NOT_ACTIVE` Plan/Apply rule? |  | project:owner | 2026-09-01T11:09:29.365757Z | 50b0ee91a5856c5d7fc02cf778f3ff53ffa573b8962c51b01ddaba2ac679d299 |
