---
schema: "agora/review-finding/v1"
id: "pre-pr-full-maven-baseline"
swarm: "delivery"
work: "cobol-python-migration"
pass: "pre-pr"
severity: "medium"
status: "waived"
policy: "verification/full-reactor"
location: "pom.xml"
created-at: "2026-08-30T13:41:56.132839Z"
decided-by: "project:owner"
decided-at: "2026-08-31T15:47:49.973154Z"
decision-reason: "Pre-existing JPMS visibility failures in renovatio-core are unchanged environmental baseline findings. Not introduced by this work and tracked separately in build infra backlog; no deterministic migration behavior regression from this work."
---

# Review finding pre-pr-full-maven-baseline

## Summary

The focused MCP tests and Python CI suite pass, but the full Maven reactor remains red on pre-existing JPMS dependency visibility failures in renovatio-core.
