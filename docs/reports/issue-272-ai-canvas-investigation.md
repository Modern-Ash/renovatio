# Issue 272 - AI Suggestions on Canvas Investigation

Date: 2026-09-12

## Finding

The Domain surface already has a durable node binding through `DomainSuggestion`:

- `targetType`
- `targetId`
- `status`

That is the correct first integration point for canvas badges because it points directly at `DomainNode.id` and is decided through the existing DomainModel suggestion flow.

`WorkbenchAiItem` is richer for audit and LLM context, but the current TypeScript surface does not expose a node binding field. Before architecture-node badges can be complete, the backend/API needs to expose either `targetNodeId`/`targetType` on `WorkbenchAiItem` or an equivalent architecture suggestion DTO.

## Implemented First Cut

- Domain diagram nodes now receive `pendingSuggestionCount`.
- `DomainClassNode` renders a compact badge for pending suggestions.
- Domain diagram toolbar includes a `Suggestions` toggle to focus the canvas on nodes that still need human review.

The existing inspector remains the decision surface. Selecting a badged node shows the suggestion queue and reuses the existing accept/edit/reject backend actions.
