# Agora session context

## Project

- Name: renovatio
- Root: `/home/faguero/dev/renovatio`

## Runtime

- Integration: `codex`
- Provider: `openai`
- Model: `configured-by-codex`
- Execution profile: `balanced`

## Responsible actor

- Identity: `project:agent`
- Kind: `ai-agent`
- Roles: `developer`
- Capabilities: `implementation`
- Represented swarm: `none`

## Executor

- Identity: `project:agent`
- Kind: `ai-agent`
- Capabilities: `implementation`
- Represented swarm: `none`
- Authority: bounded by the responsible actor's assigned roles; execution does not transfer ownership or approval authority.

## Swarm

- Id: `decision-engine-f4-dataaccess`
- Method: `spec-driven`
- Objective: Cerrar el gap de F4: hacer que GET /api/projects/{id}/data-accesses cargue y devuelva las clasificaciones reales del análisis del proyecto, manteniendo determinismo, aislamiento por proyecto y compatibilidad con el contrato existente.

## Active work

- Id: `data-access-project-runtime`
- Title: F4 · project-backed data-access classifications
- State: `implementing`
- Path: `.agora/swarms/015-decision-engine-f4-dataaccess/work/data-access-project-runtime`

## Decision snapshot

- Token: `7de8b4e4af84bd740c5f44b2e878485eab1738ca6eaed5d16053e04eae8d9799`
- Refresh: `agora work inspect --swarm $AGORA_SWARM --work $AGORA_WORK --snapshot-token 7de8b4e4af84bd740c5f44b2e878485eab1738ca6eaed5d16053e04eae8d9799`

## Required reading

- `.agora/project.md`
- `.agora/constitution.md`
- `.agora/PROTOCOL.md`
- `.agora/STANDARDS.md`
- `.agora/tools/TOOLS.md`
- `.agora/swarms/015-decision-engine-f4-dataaccess/SWARM.md`
- `.agora/methods/spec-driven/METHOD.md`
- `.agora/methods/spec-driven/PROTOCOL.md`
- `.agora/methods/spec-driven/TOOLS.md`
- `.agora/methods/spec-driven/roles/developer.md`
- `.agora/swarms/015-decision-engine-f4-dataaccess/work/data-access-project-runtime/WORK.md`
- `.agora/swarms/015-decision-engine-f4-dataaccess/work/data-access-project-runtime/artifacts.md`
- `.agora/swarms/015-decision-engine-f4-dataaccess/work/data-access-project-runtime/evidence.md`
- `.agora/swarms/015-decision-engine-f4-dataaccess/work/data-access-project-runtime/approvals.md`

## Operating rules

1. Start from the bounded decision snapshot. Read the listed policy and active-work records, then expand only to a targeted file or small range required by the next action. Never read `.agora/activity.md` or an entire event ledger for session orientation.
2. Perform only actions allowed to the assigned role and active transition.
3. Use the Agora CLI to persist state, artifacts, evidence, and material outcomes.
   Agora engine progress is emitted line-by-line on stderr; keep `AGORA_TRACE` enabled so chat hosts can relay each governed step.
4. Do not treat unrecorded conversation history as durable project state.
5. Stop when policy, permissions, or a gate cannot be satisfied.
6. Act as the executor named above; do not claim ownership or human approval on behalf of the responsible actor.
7. Report only meaningful execution milestones with `agora session progress --session $AGORA_SESSION_ID --by $AGORA_EXECUTOR --summary "..."`; never report chain-of-thought or private reasoning.
8. Keep commands quiet and bounded: prefer summaries, narrow test targets, small line ranges, and references to durable artifacts over full diffs, source trees, or build logs. Read only the selected `.agora/environments/<id>.md` immediately before an environment-aware tool operation.
