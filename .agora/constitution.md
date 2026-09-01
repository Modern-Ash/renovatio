---
schema: "agora/constitution/v1"
project: "renovatio"
status: "draft"
---

# Project constitution

## Principles

- Humans and agents follow the same role contracts.
- Every external action is attributable to an actor and role.
- Work advances only when the active Method Pack permits it.
- Project language, runtime, LLM, and development process are configuration, not core assumptions.
- Decisions, handoffs, artifacts, and evidence remain reviewable in Git.
- Production-impacting actions require an explicit project policy.
- Environment-aware Tool Runs must bind a stable project environment separately from provider
  target inputs and credentials.
- Cross-host writer coordination may use a reviewed external lease CLI, but work truth remains in
  the filesystem and Git.
- Recursive delegation must remain acyclic and within the configured maximum depth.
- Repository commits follow every active standard in `.agora/STANDARDS.md`, including Conventional
  Commits 1.0.0.

## Local amendments

Record project-specific engineering, security, compliance, and approval rules here.

### A1 — Development cycles run through the Agora governed loop

Every development cycle MUST be executed through an Agora work item inside a swarm,
advanced only via `agora work start` → `agora continue` → `agora work finish`
(or the equivalent `transition` / `criterion-satisfy` commands). "Plain SDD" —
authoring `spec.md` / `plan.md` (or equivalent design docs) and writing implementation
code outside a tracked work item — is not permitted for governed work.

- Spec and plan artifacts are produced as the work item's own artifacts and registered
  against it; they are not hand-written ahead of, or outside, the work item.
- Each acceptance criterion is satisfied and evidenced through the work item before
  the completion gate.
- The `spec-driven` Method Pack states (`drafting → clarified → planned → implementing
  → verifying → completed`) are the only legal path; no state is skipped.
- One Epic phase = one work item = one full spec → plan → implementation → verification
  cycle. Spikes may use a lighter work item but still run through the loop.
- The characterization guardrail harness (issue #122) is a completion-gate condition
  for any work item that touches the COBOL→target generation path.
