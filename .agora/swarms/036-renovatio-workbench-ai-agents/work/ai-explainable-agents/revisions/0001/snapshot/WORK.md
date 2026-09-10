---
schema: "agora/work/v1"
id: "ai-explainable-agents"
swarm: "renovatio-workbench-ai-agents"
title: "Theia 6 \u00b7 Agentes IA Renovatio y UX explicable"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"agent-catalog":"Workbench AI API and UI enumerate specialized assistants for discovery, domain, architecture, naming, equivalence, and review.","prompt-context":"Each assistant exposes a stable prompt id/version and the canonical context snapshot used to ground suggestions.","slash-tools":"The UI presents supported slash commands and their backend tool/read surface without granting direct file mutation.","human-review-boundary":"Every mutating recommendation is framed as proposal-only and requires accept/edit/reject style human confirmation.","audit-reproducibility":"AI suggestions include prompt, context hash, response hash, tool-call trace, status, and approval state for reproducibility.","ui-verification":"Frontend contract covers the governed AI area, and backend tests cover the /workbench/ai DTO."}
satisfied-criteria: ["agent-catalog","prompt-context","audit-reproducibility","slash-tools","human-review-boundary","ui-verification"]
criterion-statuses: {"agent-catalog":["specified","planned","implemented","verified","accepted"],"prompt-context":["specified","planned","implemented","verified","accepted"],"slash-tools":["specified","planned","implemented","verified","accepted"],"human-review-boundary":["specified","planned","implemented","verified","accepted"],"audit-reproducibility":["specified","planned","implemented","verified","accepted"],"ui-verification":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","implementation","verification","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 6 · Agentes IA Renovatio y UX explicable

## Description

Expose governed Renovatio AI assistants inside Theia with agent catalog, versioned prompts, slash command affordances, canonical context, audit trace, and explicit human review boundaries.

## Acceptance criteria

- [x] **agent-catalog:** Workbench AI API and UI enumerate specialized assistants for discovery, domain, architecture, naming, equivalence, and review.; stages: specified, planned, implemented, verified, accepted
- [x] **prompt-context:** Each assistant exposes a stable prompt id/version and the canonical context snapshot used to ground suggestions.; stages: specified, planned, implemented, verified, accepted
- [x] **slash-tools:** The UI presents supported slash commands and their backend tool/read surface without granting direct file mutation.; stages: specified, planned, implemented, verified, accepted
- [x] **human-review-boundary:** Every mutating recommendation is framed as proposal-only and requires accept/edit/reject style human confirmation.; stages: specified, planned, implemented, verified, accepted
- [x] **audit-reproducibility:** AI suggestions include prompt, context hash, response hash, tool-call trace, status, and approval state for reproducibility.; stages: specified, planned, implemented, verified, accepted
- [x] **ui-verification:** Frontend contract covers the governed AI area, and backend tests cover the /workbench/ai DTO.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- implementation
- verification
- review-report
