---
schema: "agora/work/v1"
id: "renovatio-cli"
swarm: "adoption-tooling"
title: "renovatio-cli: picocli CLI over the in-process core (issue #130 phase 1)"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"cli-subcommands":"renovatio binary exposes analyze, metrics, plan, apply, diff, review, report and serve subcommands that invoke the core in-process via LanguageProviderRegistry with no MCP client required.","human-readable-output":"Every subcommand prints human-readable text by default and machine-readable JSON when --json is passed.","review-checklist":"review renders manual-action-items.json as a checklist ordered by severity, showing reason, requiredHumanAction and acceptanceCondition, redacted through SensitiveValueRedactor.","chained-ids":"plan emits a planId, apply consumes it and emits a runId, and diff consumes the runId, chaining across separate CLI invocations in the same workspace.","no-regression":"mvn clean install is green and the MCP server still starts and behaves identically over HTTP and --stdio; renovatio serve delegates to it.","tested":"The module ships unit and CLI-level integration tests exercising each subcommand against the specs p1 fixture."}
satisfied-criteria: ["cli-subcommands","human-readable-output","review-checklist","chained-ids"]
criterion-statuses: {"cli-subcommands":["specified","planned","implemented"],"human-readable-output":["specified","planned","implemented"],"review-checklist":["specified","planned","implemented"],"chained-ids":["specified","planned","implemented"],"no-regression":["specified","planned"],"tested":["specified","planned","implemented"]}
required-artifacts: ["spec","implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
---

# renovatio-cli: picocli CLI over the in-process core (issue #130 phase 1)

## Description

Issue #130 phase 1. New Maven module renovatio-cli: a picocli binary with subcommands 1:1 with the COBOL tools (analyze, metrics, plan, apply, diff, review, report, serve) that boots the Spring context headless and calls LanguageProviderRegistry in-process. Human-readable output by default, --json for scripting. 'review' renders manual-action-items.json as a severity-ordered checklist. 'serve' delegates to the existing MCP server. No changes to generation/transpilation logic.

## Acceptance criteria

- [x] **cli-subcommands:** renovatio binary exposes analyze, metrics, plan, apply, diff, review, report and serve subcommands that invoke the core in-process via LanguageProviderRegistry with no MCP client required.; stages: specified, planned, implemented
- [x] **human-readable-output:** Every subcommand prints human-readable text by default and machine-readable JSON when --json is passed.; stages: specified, planned, implemented
- [x] **review-checklist:** review renders manual-action-items.json as a checklist ordered by severity, showing reason, requiredHumanAction and acceptanceCondition, redacted through SensitiveValueRedactor.; stages: specified, planned, implemented
- [x] **chained-ids:** plan emits a planId, apply consumes it and emits a runId, and diff consumes the runId, chaining across separate CLI invocations in the same workspace.; stages: specified, planned, implemented
- [ ] **no-regression:** mvn clean install is green and the MCP server still starts and behaves identically over HTTP and --stdio; renovatio serve delegates to it.; stages: specified, planned
- [x] **tested:** The module ships unit and CLI-level integration tests exercising each subcommand against the specs p1 fixture.; stages: specified, planned, implemented

## Required artifacts

- spec
- implementation-plan
- test-report
