# Implementation

Implemented in branch `agora/theia-ai-explainable-agents`.

Backend:

- `WorkbenchAiDto` now includes agents, prompts, slash commands, context snapshot, tool policy, reviewable items, audit trail, and explicit limits.
- `WorkbenchAiService` composes the governed AI surface from existing workbench services and decision-layer suggestions.
- Context and audit hashes are deterministic enough for repeatable review and traceability.
- Tool policy explicitly disallows direct file writes and requires human confirmation for mutating recommendations.

Frontend:

- The AI workbench area now renders specialized agents, prompt catalog, slash commands, canonical context, reviewable suggestions, audit trail, and policy limits.
- Permission-denied and empty states are handled for the AI area.
- The stale `ISSUE 181` shell label was replaced with a neutral Theia workbench label.
