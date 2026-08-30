---
schema: "agora/review-finding/v1"
id: "pre-pr-full-maven-baseline"
swarm: "delivery"
work: "cobol-python-migration"
pass: "pre-pr"
severity: "medium"
status: "open"
policy: "verification/full-reactor"
location: "pom.xml"
created-at: "2026-08-30T13:41:56.132839Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding pre-pr-full-maven-baseline

## Summary

The focused MCP tests and Python CI suite pass, but the full Maven reactor remains red on pre-existing JPMS dependency visibility failures in renovatio-core.
