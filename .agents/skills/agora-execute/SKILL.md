---
name: "agora-execute"
description: "Execute a permitted transition step for an assigned Agora role"
---

# Execute governed work

When a chat host launches Agora, set `AGORA_TRACE=compact` (or use the global
`agora --trace compact ...` option) and relay each line from stderr so the user can see Core's
governed phases without mixing them into JSON output.

Default to a token-slim Agora loop. If the active swarm/work item is already known from the user's
request, local context, or an `AGORA_CONTEXT`, read only the relevant `WORK.md`, `artifacts.md`,
`evidence.md`, and narrowly necessary status/readiness output. Do not run broad discovery commands
(`agora status`, `agora next`, global list commands, or `agora validate`) as a reflex.

Use `agora next --actor "$AGORA_ACTOR"` only when the current assignment is unknown or ambiguous.
When launched through `agora run`, read the context at `AGORA_CONTEXT` before changing the project.
Record at least one governed transition, artifact, evidence, approval, block, or delegation outcome
before exiting successfully only when the turn actually advances governed state. For implementation
work, prefer one final artifact/evidence entry after verification instead of recording every small
step. A bounded `--until-blocked` controller stops when no durable progress is detected. Never select
a rework edge merely to avoid a higher-priority human decision.
Treat the timeout and output limits in `AGORA_SESSION` as immutable execution policy. The controller
records bounded process output in the session `RESULT.md`; place material outcomes in governed work
artifacts and evidence rather than relying on that process log.

Identify the active swarm, actor, assignment, work item, and current Method Pack state with the
least expensive source that is reliable for the turn. Inspect outgoing transition edges only before
mutating Method Pack state, blocking/resuming/cancelling work, handing off, approving, or claiming a
gate is satisfied. Respect WIP limits and gates. Persist material decisions, interactions,
artifacts, evidence, and approvals; skip durable writes for ordinary code exploration that produces
no stable decision or verification result. Invoke installed external operations through
`agora tool invoke` so their attribution and results are durable. When an operation requires an environment, select a policy from
`.agora/environments`, confirm the assigned role permits it, and satisfy its approvals and evidence.
When a runtime or reviewed adapter reports measured resource consumption, append it with
`agora usage add` and cite the authoritative telemetry reference. Never estimate or invent usage.
Check `agora usage status --swarm <swarm> --work <work>` before allocating or launching bounded
work only when a durable budget exists or the next operation has meaningful measured cost.
When work is delegated, read the related `DELEGATION.md` and act only within its parent or child
contract. Do not invent a transition or bypass a gate.

When repository history is required, read `.agora/STANDARDS.md` and use the governed
`repository/commit` operation with a Conventional Commits 1.0.0 message. Do not bypass its input
validation with an ungoverned Git command.

If active work cannot proceed, use an authorized block with an explicit reason instead of inventing
a Method Pack state. Do not mutate blocked or cancelled work. Resume only after its stated blocker is
resolved. Treat delegation rejection as child authority and delegation cancellation as parent
authority; neither operation permits silently rewriting independently owned child work.

Execution request: `$ARGUMENTS`
