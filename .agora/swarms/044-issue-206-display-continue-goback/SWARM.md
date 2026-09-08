---
schema: "agora/swarm/v1"
id: "issue-206-display-continue-goback"
method: "spec-driven"
status: "running"
branch: "agora/issue-206-control-io-verbs"
required-roles: ["spec-owner","developer"]
assignments: {"spec-owner":"project:owner","developer":"project:agent"}
---

# Swarm issue-206-display-continue-goback

## Objective

Issue #206 (cycle 1): make the COBOL->Java translator render DISPLAY, CONTINUE, GOBACK and STOP RUN instead of silently dropping them. DISPLAY '<lit>' A B -> System.out.println("<lit>" + a + b); CONTINUE -> no-op; GOBACK / STOP RUN -> return the output DTO. Every other still-unrecognised PROCEDURE DIVISION statement must become a visible // not-translated comment (no silent drop, no invalid Java). Add characterization fixtures. Re-run the CardDemo coverage report (#217) to show the delta. No change to numeric/type mapping or to already-working verbs.

## Assignments

| Role | Actor |
| --- | --- |
| spec-owner | project:owner |
| developer | project:agent |
