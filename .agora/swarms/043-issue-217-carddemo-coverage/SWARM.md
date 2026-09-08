---
schema: "agora/swarm/v1"
id: "issue-217-carddemo-coverage"
method: "spec-driven"
status: "running"
branch: "agora/issue-218-manifest-consistency"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm issue-217-carddemo-coverage

## Objective

Fix issue #217: run the Renovatio pipeline (parse -> IR -> emit Java -> javac) over the whole AWS CardDemo COBOL corpus and publish a reproducible coverage report. Vendor the CardDemo app COBOL/copybook/JCL sources into the repo as a test corpus. Emit docs/reports/carddemo-coverage.md + a committed JSON for run-over-run tracking: % programs that parse, % that generate Java that compiles, top unsupported verbs/constructs by frequency, breakdown by subsystem (batch / CICS / DB2 / IMS+MQ). Identify the 3 simplest batch programs (no CICS/DB2) as targets for the E2E issue #216. Runnable via one documented command and in CI.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
