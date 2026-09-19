---
name: "agora-status"
description: "Inspect and validate durable Agora project state"
---

# Inspect Agora state

When invoked from chat or another non-TTY host, set `AGORA_TRACE=compact` and relay Agora's stderr
lines; keep stdout intact for the structured result.

Prefer the cheapest query that answers the user. If the user names a swarm/work item or the relevant
work is already known, inspect only that work's durable files or run a targeted command such as
`agora work show`, `agora work inspect`, `agora work readiness`, or `agora event list --swarm ...`
when available. Do not run the broad `agora status` + `agora next` + `agora validate` sequence just
to regain context for a known work item.

Use broad discovery (`agora status`, `agora next`, `agora inbox`, and domain `list` commands) only
when selecting unknown work, reporting overall project state, diagnosing queue/WIP issues, or
answering a question that needs global state. Use `agora validate` only before relying on
cross-record references, before completion/release-quality claims, or when durable records appear
inconsistent. Treat validation errors as durable-state problems: report the exact code and path, and
do not silently rewrite or infer missing records.

For token-sensitive turns, summarize the minimum relevant state: work id, Method Pack state,
operational status, blockers/gate gaps, and the next concrete action. Avoid pasting full JSON unless
the user asks for it or a downstream command needs the exact structured result. Distinguish Method
Pack state from work `operational-status`. Inspect nested status changes before explaining a block,
resumption, rejection, or cancellation.

Query target: `$ARGUMENTS`
