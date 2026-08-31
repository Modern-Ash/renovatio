# Agora session context

## Project

- Name: renovatio
- Root: `/home/faguero/dev/renovatio`

## Runtime

- Integration: `codex`
- Provider: `openai`
- Model: `configured-by-codex`

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

- Id: `ai-modernization`
- Method: `spec-driven`
- Objective: Deliver a reproducible three-pass COBOL modernization pipeline: deterministic parsers and OpenRewrite recipes own core semantics; governed, content-addressed LLM enrichment operates only on IR; optional idiomatic polish produces review-only diffs with deterministic fallback.

## Active work

- Id: `characterization-guardrails`
- Title: Characterization harness and non-negotiable gates
- State: `implementing`
- Path: `.agora/swarms/002-ai-modernization/work/characterization-guardrails`

## Required reading

- `.agora/project.md`
- `.agora/activity.md`
- `.agora/constitution.md`
- `.agora/PROTOCOL.md`
- `.agora/STANDARDS.md`
- `.agora/tools/TOOLS.md`
- `.agora/swarms/002-ai-modernization/SWARM.md`
- `.agora/swarms/002-ai-modernization/events.md`
- `.agora/methods/spec-driven/METHOD.md`
- `.agora/methods/spec-driven/PROTOCOL.md`
- `.agora/methods/spec-driven/TOOLS.md`
- `.agora/methods/spec-driven/roles/developer.md`
- `.agora/environments/README.md`
- `.agora/swarms/002-ai-modernization/work/characterization-guardrails/WORK.md`
- `.agora/swarms/002-ai-modernization/work/characterization-guardrails/artifacts.md`
- `.agora/swarms/002-ai-modernization/work/characterization-guardrails/evidence.md`
- `.agora/swarms/002-ai-modernization/work/characterization-guardrails/approvals.md`

## Operating rules

1. Read every available file listed above before acting.
2. Perform only actions allowed to the assigned role and active transition.
3. Use the Agora CLI to persist state, artifacts, evidence, and material outcomes.
   Agora engine progress is emitted line-by-line on stderr; keep `AGORA_TRACE` enabled so chat hosts can relay each governed step.
4. Do not treat unrecorded conversation history as durable project state.
5. Stop when policy, permissions, or a gate cannot be satisfied.
6. Act as the executor named above; do not claim ownership or human approval on behalf of the responsible actor.
7. Report only meaningful execution milestones with `agora session progress --session $AGORA_SESSION_ID --by $AGORA_EXECUTOR --summary "..."`; never report chain-of-thought or private reasoning.
