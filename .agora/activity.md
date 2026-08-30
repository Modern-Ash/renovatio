---
schema: "agora/activity-ledger/v1"
---

# Activity ledger

Append-only project chronology. Raw output remains in linked session and Tool Run records.
- 2026-08-30T13:28:24.107001Z | project.initialized | actor=- swarm=- work=- session=- tool-run=- source=repo://.agora/project.md | Initialized project with integration=codex method=spec-driven
- 2026-08-30T13:28:24.136639Z | swarm.created | actor=- swarm=delivery work=- session=- tool-run=- source=repo://.agora/swarms/001-delivery/events.md | branch=agora/delivery
- 2026-08-30T13:28:24.137366Z | swarm.actor-assigned | actor=project:owner swarm=delivery work=- session=- tool-run=- source=repo://.agora/swarms/001-delivery/events.md | role=spec-owner actor=project:owner
- 2026-08-30T13:28:24.137978Z | swarm.actor-assigned | actor=project:agent swarm=delivery work=- session=- tool-run=- source=repo://.agora/swarms/001-delivery/events.md | role=developer actor=project:agent
- 2026-08-30T13:28:24.138230Z | adopt.completed | actor=- swarm=delivery work=- session=- tool-run=- source=repo://.agora/swarms/001-delivery/SWARM.md | Created starter team and ready swarm with method=spec-driven; secure=false
- 2026-08-30T13:29:06.237699Z | work.created | actor=project:owner swarm=delivery work=cobol-python-migration session=- tool-run=- source=repo://.agora/swarms/001-delivery/work/cobol-python-migration/events.md | state=drafting actor=project:owner
- 2026-08-30T13:29:24.879387Z | artifact.added | actor=project:owner swarm=delivery work=cobol-python-migration session=- tool-run=- source=repo://.agora/swarms/001-delivery/work/cobol-python-migration/events.md | kind=spec uri=specs/1-cobol-python-migration/spec.md actor=project:owner
