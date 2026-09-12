# @renovatio/diagram-canvas

Reusable drag-and-drop diagram canvas ([React Flow](https://reactflow.dev), via `@xyflow/react`) for the Renovatio Workbench.

This package is deliberately dumb: it renders a `DiagramModel` (nodes + edges as plain view-model objects, see `src/common/diagram-protocol.ts`) and reports every user interaction back as a `DiagramEvent` (`nodeMoved`, `nodeSelected`, `edgeCreated`, `nodesPruned`). It has **no knowledge** of:

- Renovatio's real domain types (`DomainModel`, `ArchitectureProfileDraft`, ...)
- the backend API (`backendUrl`, `fetch`)
- Theia (no `@theia/core` dependency)

That mapping — real model → `DiagramModel`, and `DiagramEvent` → a mutation of the real draft — lives in the **host** widget, `@renovatio/core-ui` (`renovatio-shell-widget.tsx`). See:

- Issue #265 — Domain canvas, "classes" (UML) mode
- Issue #266 — Domain canvas, DER mode
- Issue #267 — Architecture canvas, package/layer graph

## Usage from `@renovatio/core-ui`

Import directly — this is a workspace package, not something fetched over the network:

```tsx
import { DiagramCanvas, DiagramModel, DiagramEvent } from '@renovatio/diagram-canvas/lib/browser';

const model: DiagramModel = domainModelToDiagram(this.domainDraft);

<DiagramCanvas
    model={model}
    onEvent={this.handleDomainDiagramEvent}
    nodeTypes={{ domainClass: DomainClassNode }}
    nodeTypeFor={node => node.kind === 'ENTITY' ? 'domainClass' : 'default'}
/>
```

`nodeTypes`/`nodeTypeFor` let each host (Domain UML, Domain DER, Architecture) register its own React Flow custom node renderer without forking or extending this package — the canvas core ships no custom node type of its own beyond React Flow's built-in default box.

## Why a separate workspace package instead of more code in `renovatio-core-ui`

1. **Reuse without Theia.** The same component is meant to be reused unmodified inside a VS Code Custom Editor webview (issue #270), which does not have `@theia/core` available at all.
2. **Testability boundary.** Contract tests here (`tests/contract.test.mjs`) assert this package never reaches for `fetch`/`backendUrl`/domain types — a regression here would mean a host concern leaked into a component three unrelated diagram modes share.

## Scripts

- `npm run build` — `tsc -b` (declaration output only; this package ships TypeScript compiled to CommonJS, consistent with `@renovatio/core-ui`).
- `npm test` — `node --test tests/*.test.mjs`, following the same source-contract testing convention already used by `@renovatio/core-ui/tests/contract.test.mjs` (regex assertions over source, no DOM rendering harness is set up in this workspace — see the note below).

## Known limitation: no DOM-rendering test yet

This workspace has no `jsdom`/`@testing-library` setup (unlike `renovatio-ui`, a different, unrelated app). The contract tests here verify the component's *shape* (what it renders, what events it wires up, what it imports) by asserting against source text, the same way every existing `@renovatio/core-ui` contract test does — they do not mount a real DOM and simulate a drag. Real interactive verification (does dragging a node actually move it and fire `nodeMoved`) is intended to happen through the Playwright e2e suite (`renovatio-workbench/e2e`) once this canvas is wired into a live widget (issue #265), where a real browser is already available. If a future contributor adds `jsdom` to this workspace, replacing/augmenting these contract tests with a real render+drag test would tighten this — see issue #264 acceptance criteria on GitHub for the original ask.
