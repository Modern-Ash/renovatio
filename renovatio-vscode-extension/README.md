# Renovatio Diagram Editor for VS Code

This package exposes the shared `@renovatio/diagram-canvas` React Flow component as a standard VS Code Custom Editor.

It supports standalone local files:

- `*.renovatio-domain.json`
- `*.renovatio-arch.json`

The local Renovatio artifact files are the source of truth for editor state. The extension can now initialize a workspace manifest at `.renovatio/workspace.renovatio.json`; backend synchronization remains a separate workflow concern.

## Workspace Manifest

Run `Renovatio: Initialize Workspace` from the Command Palette to create:

```text
.renovatio/workspace.renovatio.json
```

The manifest is a workspace-relative, versioned contract for agents, developers and the Renovatio backend. It captures:

- `projectId`: stable local project identity.
- `source`: legacy language, source roots, include globs and exclude globs.
- `targets`: target language roots plus optional package/framework metadata.
- `artifacts`: paths for domain, persistence, architecture, migration map and evidence output.
- `backend`: API URL, environment and whether local process control is allowed.
- `llm`: reverse-engineering provider, model, prompt profile, temperature, token limit and cache behavior.

All paths in `source.roots`, `targets[].root` and `artifacts` should be relative to the VS Code workspace folder. The extension contributes JSON Schema validation for:

- `.renovatio/workspace.renovatio.json`
- `*.renovatio-domain.json`
- `*.renovatio-arch.json`
- `migration-map.renovatio.json`

The manifest takes precedence for new two-way workbench features. Existing VS Code settings still act as fallback/defaults when the manifest does not exist:

- `renovatio.cobolRoots`
- `renovatio.generatedRoot`
- `renovatio.generatedRoots`
- `renovatio.targetLanguage`
- `renovatio.targetPackage`
- `renovatio.backendUrl`

Useful commands:

- `Renovatio: Initialize Workspace`
- `Renovatio: Open Workspace Manifest`
- `Renovatio: Validate Workspace`
- `Renovatio: Format Artifacts`

## Backend And LLM Control

The Renovatio Activity Bar includes a `Backend` view backed by `.renovatio/workspace.renovatio.json`.

It shows:

- Backend URL, environment, last health check, backend version and last error.
- LLM provider, active model, fallback model, prompt profile, cache state and smoke-test result.
- Local/dev server control actions.

Process-control commands are safety gated:

- `backend.environment` must be `local` or `dev`.
- `backend.allowLocalProcessControl` must be `true`.
- Every command displays the exact shell command and asks for confirmation before execution.

Backend commands may be configured in the manifest under `backend.commands`. Missing backend endpoints are reported as unsupported instead of faking success.

## Build

```sh
cd renovatio-workbench
npm run build --workspace @renovatio/diagram-canvas

cd ../renovatio-vscode-extension
npm install
npm run build
npm run package
```

The package step emits a `.vsix` that can be installed with:

```sh
code --install-extension renovatio-vscode-extension-0.1.0.vsix
```

## Editing Behavior

The webview receives a `DiagramModel` and emits `DiagramEvent` messages. The extension host applies each edit to the backing `TextDocument` through `WorkspaceEdit`, so VS Code owns dirty tracking, save, undo and redo.

Dragging a node writes layout metadata:

- Domain files: `layout[nodeId] = { x, y }`
- Architecture files: `profile.layout[nodeId] = { x, y }`
- Plain diagram files: `nodes[].x/y`

Pruning a node writes `excludedNodeIds` metadata where the domain or architecture schema supports it.
