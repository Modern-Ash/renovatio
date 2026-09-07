---
schema: "agora/event-archive/v1"
source-ledger: "repo://.agora/activity.md"
swarm: "java-21-migration"
archived-at: "2026-09-07T22:00:00Z"
---

# Java 21 migration event archive

The original `.agora/swarms/007-java-21-migration/events.md` records referenced by the project activity ledger are absent from repository history. This archive preserves the recovered project-ledger entries as the durable source for those historical activity records.

## Recovered events

- 2026-09-01T20:59:25.347765Z | swarm.created | actor=- swarm=java-21-migration work=- session=- tool-run=- source=repo://.agora/activity.md | branch=agora/java-21-migration
- 2026-09-01T20:59:28.831384Z | swarm.actor-assigned | actor=project:owner swarm=java-21-migration work=- session=- tool-run=- source=repo://.agora/activity.md | role=spec-owner actor=project:owner
- 2026-09-01T20:59:29.441178Z | swarm.actor-assigned | actor=project:agent swarm=java-21-migration work=- session=- tool-run=- source=repo://.agora/activity.md | role=developer actor=project:agent
- 2026-09-01T20:59:55.275042Z | work.created | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | state=drafting actor=project:owner
- 2026-09-01T21:01:32.678105Z | artifact.added | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | kind=spec uri=file:///home/faguero/dev/renovatio/.agora/swarms/007-java-21-migration/artifacts/spec-java-21-migration.md actor=project:owner
- 2026-09-01T21:02:04.874198Z | work.clarified-advisory | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | questions=5 actor=project:owner
- 2026-09-01T21:03:40.469166Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=pom-updated actor=project:owner stage=specified
- 2026-09-01T21:03:41.838623Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=dockerfiles-updated actor=project:owner stage=specified
- 2026-09-01T21:03:42.522693Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=cicd-updated actor=project:owner stage=specified
- 2026-09-01T21:03:43.157630Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=deps-compatible actor=project:owner stage=specified
- 2026-09-01T21:03:43.746651Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=tests-pass actor=project:owner stage=specified
- 2026-09-01T21:03:44.506345Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=mcp-server-starts actor=project:owner stage=specified
- 2026-09-01T21:03:45.376830Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=docs-updated actor=project:owner stage=specified
- 2026-09-01T21:03:49.090510Z | work.transitioned | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | from=drafting to=clarified actor=project:owner
- 2026-09-01T21:03:49.092296Z | swarm.status-changed | actor=- swarm=java-21-migration work=- session=- tool-run=- source=repo://.agora/activity.md | from=ready to=running
- 2026-09-01T21:04:20.685250Z | artifact.added | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | kind=implementation-plan uri=file:///home/faguero/dev/renovatio/.agora/swarms/007-java-21-migration/artifacts/implementation-plan-java-21.md actor=project:agent
- 2026-09-01T21:04:32.424054Z | work.transitioned | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | from=clarified to=planned actor=project:agent
- 2026-09-01T21:05:30.880987Z | work.transitioned | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | from=planned to=implementing actor=project:agent
- 2026-09-01T21:09:15.157650Z | artifact.added | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | kind=evidence uri=file:///home/faguero/dev/renovatio/.agora/swarms/007-java-21-migration/artifacts/evidence-java-21-migration.md actor=project:agent
- 2026-09-01T21:09:25.404232Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=pom-updated actor=project:agent stage=implemented
- 2026-09-01T21:09:25.638628Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=dockerfiles-updated actor=project:agent stage=implemented
- 2026-09-01T21:09:25.879025Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=cicd-updated actor=project:agent stage=implemented
- 2026-09-01T21:09:26.128324Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=deps-compatible actor=project:agent stage=implemented
- 2026-09-01T21:09:26.366465Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=tests-pass actor=project:agent stage=implemented
- 2026-09-01T21:09:26.615009Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=mcp-server-starts actor=project:agent stage=implemented
- 2026-09-01T21:09:26.867211Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=docs-updated actor=project:agent stage=implemented
- 2026-09-01T21:11:38.063038Z | work.transitioned | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | from=implementing to=verifying actor=project:agent
- 2026-09-01T21:11:44.071446Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=pom-updated actor=project:agent stage=verified
- 2026-09-01T21:11:44.321073Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=dockerfiles-updated actor=project:agent stage=verified
- 2026-09-01T21:11:44.579886Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=cicd-updated actor=project:agent stage=verified
- 2026-09-01T21:11:44.829566Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=deps-compatible actor=project:agent stage=verified
- 2026-09-01T21:11:45.085025Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=tests-pass actor=project:agent stage=verified
- 2026-09-01T21:11:45.331069Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=mcp-server-starts actor=project:agent stage=verified
- 2026-09-01T21:11:45.586356Z | work.criterion-stage-marked | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=docs-updated actor=project:agent stage=verified
- 2026-09-01T21:12:30.047962Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=pom-updated actor=project:owner stage=accepted
- 2026-09-01T21:12:30.269376Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=dockerfiles-updated actor=project:owner stage=accepted
- 2026-09-01T21:12:30.485930Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=cicd-updated actor=project:owner stage=accepted
- 2026-09-01T21:12:30.707815Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=deps-compatible actor=project:owner stage=accepted
- 2026-09-01T21:12:30.922952Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=tests-pass actor=project:owner stage=accepted
- 2026-09-01T21:12:31.154832Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=mcp-server-starts actor=project:owner stage=accepted
- 2026-09-01T21:12:31.381146Z | work.criterion-stage-marked | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | criterion=docs-updated actor=project:owner stage=accepted
- 2026-09-01T21:12:36.528186Z | approval.added | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | role=spec-owner actor=project:owner delegation=none
- 2026-09-01T21:13:05.990313Z | evidence.added | actor=project:agent swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | id=evidence-000001 type=build result=success revision=1 actor=project:agent
- 2026-09-01T21:13:11.585726Z | work.transitioned | actor=project:owner swarm=java-21-migration work=java-21-migration session=- tool-run=- source=repo://.agora/activity.md | from=verifying to=completed actor=project:owner
- 2026-09-01T21:13:11.588729Z | swarm.status-changed | actor=- swarm=java-21-migration work=- session=- tool-run=- source=repo://.agora/activity.md | from=running to=completed
