# Renovatio Diagram Editor for VS Code

This package exposes the shared `@renovatio/diagram-canvas` React Flow component as a standard VS Code Custom Editor.

It supports standalone local files:

- `*.renovatio-domain.json`
- `*.renovatio-arch.json`

The local Renovatio artifact files are the source of truth for editor state. The extension can now initialize a workspace manifest at `.renovatio/workspace.renovatio.json`; backend synchronization remains a separate workflow concern.

This workbench documentation belongs to epic #281 and reflects the implemented child tickets #283, #284, #290, #291 and #292.

## Quick Start

```sh
cd renovatio-workbench
npm run build --workspace @renovatio/diagram-canvas

cd ../renovatio-vscode-extension
npm install
npm test
npm run build
npm run package
```

Install the emitted `.vsix`, open a COBOL/JCL workspace in VS Code, then run:

1. `Renovatio: Initialize Workspace` to create `.renovatio/workspace.renovatio.json`.
2. `Renovatio: Open Backend Settings` and `Renovatio: Configure LLM Model` to confirm backend URL, environment, provider, model and prompt profile.
3. `Renovatio: Run Backend And LLM Checks` to smoke test backend health and reverse-engineering configuration.
4. `Renovatio: Analyze VS Code Workspace` or the explicit COBOL source-root commands from the `Discovery` view.
5. `Renovatio: Open Native Domain Diagram`, `Renovatio: Open Native Architecture Diagram` and `Renovatio: Open Migration Map` to review generated artifacts.
6. `Renovatio: Preview Migration Diff`, approve or reject the change set, then `Renovatio: Apply Approved Changes`.
7. `Renovatio: Export Evidence Bundle` for reviewer and agent handoff.

For a five-minute evaluation run, use `Renovatio: Install Demo Workspace Assets` before the checks. It writes a small local-first fixture workspace with COBOL, copybook, generated Java, models, migration map and evidence files.

## Activity Bar Workflow

The Renovatio Activity Bar is organized around the modernization workflow:

- `Workspace`: active project, manifest status, source roots, target roots and setup actions.
- `Discovery`: COBOL/JCL inventory, inferred datasets and analysis output.
- `Models`: domain, persistence and architecture model entry points, including native diagrams.
- `Migration`: migration map status, source-to-target traceability actions and generated-code workflow commands.
- `Evidence`: current analysis evidence, reports, risks and evidence bundle export entry point.
- `Backend`: backend health, environment, LLM model configuration and local/dev server controls.

Each section has an actionable empty state so an evaluator can start from an empty workspace, while Renovatio developers still have expert commands in the Command Palette.

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

Minimal manifest shape:

```json
{
  "version": "1",
  "projectId": "carddemo",
  "source": { "language": "cobol", "roots": ["src/mainframe"] },
  "targets": [{ "language": "java", "root": "generated/java" }],
  "backend": { "url": "http://127.0.0.1:8081", "environment": "local", "allowLocalProcessControl": true },
  "llm": { "provider": "openai", "model": "gpt-4.1", "promptProfile": "renovatio-cobol-reverse-engineering-v1" }
}
```

## 5-Minute Evaluator Flow

Open `Renovatio: Open 5-Minute Evaluator Guide` from the Command Palette or the Renovatio Activity Bar.

For a fresh folder:

1. Run `Renovatio: Install Demo Workspace Assets`.
2. Run `Renovatio: Run Backend And LLM Checks`.
3. Run `Renovatio: Analyze VS Code Workspace`.
4. Open `Renovatio: Open Native Domain Diagram`.
5. Open `Renovatio: Open Migration Map`.
6. Run `Renovatio: Export Evidence Bundle`.

The demo installer writes a small manifest, COBOL source, copybook, generated Java sample, model artifacts, migration map and evidence summary into the current workspace. Backend and LLM identity are visible before analysis from the `Backend` view and the evaluator guide. Offline or unsupported backend endpoints are reported as status, not as a blocker for local artifact review.

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

For staging and production manifests, keep `backend.allowLocalProcessControl` disabled. Start, stop and restart commands are intended only for local/dev evaluator workspaces where the manifest explicitly opts in and VS Code shows the exact shell command before execution.

## Optional Backend Sync

Workspace artifacts remain local-first. Backend synchronization is opt-in through the manifest `sync` block and is disabled by default.

When enabled, the plugin tracks local and backend revisions for:

- `domainModel`
- `persistenceModel`
- `architecture`
- `migrationMap`

Supported sync modes:

- `manual`: users run explicit status, compare, pull or push commands.
- `pull-on-open`: opening a synced artifact checks backend state and prompts before replacing local content.
- `push-on-save`: saving a synced artifact pushes with an expected-revision guard and refuses stale overwrites.

Conflict states are surfaced in the status bar, Command Palette and `Renovatio Sync` output channel: `clean`, `local-changed`, `remote-changed`, `both-changed`, `remote-unavailable` and `schema-mismatch`. Pulls create backups under `.renovatio/backups/`; compares write temporary remote snapshots under `.renovatio/sync-preview/`.

Useful commands:

- `Renovatio: Sync Status`
- `Renovatio: Compare Local And Backend Artifact`
- `Renovatio: Pull Artifact From Backend`
- `Renovatio: Push Artifact To Backend`
- `Renovatio: Resolve Sync Conflict`

## Migration Map

Run `Renovatio: Create Migration Map` after initializing a workspace to create the manifest-defined artifact, usually:

```text
.renovatio/migration-map.renovatio.json
```

The migration map is Renovatio's durable two-way traceability contract. Each entry links a legacy source location to Renovatio semantic/domain/architecture ids, target code, evidence and the latest human or agent decision.

Supported entry statuses:

- `proposed`
- `accepted`
- `generated`
- `manually-edited`
- `stale-source`
- `stale-target`
- `needs-review`
- `rejected`

Supported entry kinds:

- `program`
- `paragraph`
- `section`
- `copybook`
- `record`
- `field`
- `jcl-job`
- `jcl-step`
- `business-rule`
- `dataset`
- `table`
- `test-fixture`

Useful commands:

- `Renovatio: Create Migration Map`
- `Renovatio: Open Migration Map`
- `Renovatio: Validate Migration Map`
- `Renovatio: Format Migration Map`

Migration map paths are workspace-relative. Validation reports malformed entries and missing source or target files in VS Code Problems.

Minimal migration entry:

```json
{
  "id": "program:CARDDEMO",
  "kind": "program",
  "source": { "language": "cobol", "path": "src/mainframe/CARDDEMO.cbl" },
  "target": { "language": "java", "path": "generated/java/src/main/java/com/example/CardDemoService.java" },
  "status": "needs-review",
  "confidence": 0.84,
  "evidence": [".renovatio/evidence/evaluator-summary.md"]
}
```

## Artifact Diagnostics

Renovatio publishes workspace-aware diagnostics to VS Code Problems for the manifest, migration map and mapped files.

Manifest diagnostics cover:

- required project, source, target, artifact, backend and LLM fields.
- unsupported source or target languages.
- duplicate target roots and paths outside the workspace.
- invalid backend URLs and unsafe local process control outside local/dev environments.
- missing LLM provider, model or prompt profile configuration.

Migration map diagnostics cover:

- duplicate entry ids, unknown statuses and invalid ranges.
- missing source, target or evidence files.
- generated or accepted entries without target output.
- generated entries without evidence.
- missing hashes that prevent freshness checks.

When an entry records `source.hash` or `target.hash`, the extension compares it with the current file content. Changed source or target files get stale-state warnings directly in the editor, and the migration map points to the affected entry so users can reconcile the two-way trace.

## Editor Navigation

When `.renovatio/migration-map.renovatio.json` exists, COBOL/JCL and generated Java/Python/Node editors get native Renovatio navigation:

- source files can open mapped target code, migration evidence, domain nodes and generated diffs from CodeLens.
- target files can open legacy source, evidence, manual-refinement marking and reconciliation actions from CodeLens.
- hovers show status, confidence, mapped source/target path, evidence count, last decision and stale warnings.

The first implementation matches exact workspace-relative `source.path` and `target.path` entries. Ranged entries use the mapped range; file-level entries appear on the first line. Missing migration maps stay silent, and missing mapped files offer to open the migration map.

## Preview And Apply Workflow

`Renovatio: Preview Migration Diff` creates a reviewable change set under:

```text
.renovatio/changesets/
```

The workflow is approval-gated:

- preview reads the workspace manifest, backend/LLM configuration and migration map.
- backend dry-run is attempted when available; otherwise the extension writes an explicit local preview change set so the workflow remains testable offline.
- each change records target path, kind, status, before/after hashes, diff metadata and migration entry ids.
- `Renovatio: Open Change Set` opens a native VS Code side-by-side diff.
- `Renovatio: Approve Change` and `Renovatio: Reject Change` update the persisted change set.
- `Renovatio: Apply Approved Changes` writes only approved changes, blocks conflicts when files changed after preview, and updates migration map target hashes, evidence links and generated status.
- `Renovatio: Reconcile Generated Code` refreshes target hashes after intentional manual edits.

Generated code is never applied from preview alone. Every write goes through explicit approval and VS Code workspace file APIs.

Change sets record the proposed target path, before/after hashes, entry ids, status and diff metadata. Applying changes requires an approved status and blocks if the target file changed after preview.

## Evidence Bundles

`Renovatio: Export Evidence Bundle` writes an auditable bundle under:

```text
.renovatio/evidence-bundles/
```

Each bundle includes:

- `manifest.json` with backend, LLM, artifact, warning, risk and summary metadata.
- `workspace.renovatio.json`.
- `migration-map.renovatio.json` when present.
- domain, persistence and architecture model artifacts when present.
- persisted change sets and diffs from `.renovatio/changesets/`.
- evidence files referenced by the migration map or stored under the manifest evidence directory.
- `checksums.txt` for every included file.
- `summary.md` for reviewers and handoff.

Missing optional artifacts are listed as warnings. Missing required artifacts block export unless the user explicitly chooses a partial bundle. `Renovatio: Open Latest Evidence Bundle` opens the latest summary, and `Renovatio: Copy Evidence Summary` copies `summary.md` to the clipboard.

Evidence bundle manifests capture the workspace, migration map, diagram artifacts, evidence files, backend identity, LLM identity and warnings used during review. This is the primary handoff artifact for future agents.

## Agent Handoff Guide

Implementation entry points:

- Commands and activation events live in `package.json`; command handlers are registered from `src/extension.ts`.
- Activity Bar views are implemented by `src/views.ts` and share manifest state from `src/workspaceManifest.ts`.
- Backend and LLM controls live in `src/backend.ts`; keep local process control gated by manifest environment and explicit confirmation.
- Migration map contracts live in `src/migrationMap.ts`; testable indexing, hover and hash helpers live in `src/workbenchCore.ts`.
- CodeLens, hover and source-target navigation live in `src/navigation.ts`.
- Change-set preview/apply behavior lives in `src/changeSet.ts`.
- Evidence bundle export lives in `src/evidence.ts`.
- Diagram parsing and edit application live in `src/model.ts`; shared rendering code lives in `renovatio-workbench/extensions/renovatio-diagram-canvas`.
- JSON Schemas live under `schemas/`; add or update matching examples under `examples/` and tests under `test/fixtures/workspace-basic/`.

Verification before opening a PR:

```sh
cd renovatio-vscode-extension
npm test
npm run build
npm run package

cd ..
./mvnw -B verify -Djacoco.skip=true -Dexec.skip=true
(cd renovatio-ui && npm run build)
(cd renovatio-workbench && npm run build)
```

Common pitfalls:

- Do not let backend sync silently overwrite local artifacts; pulls must create backups and pushes must use revision guards.
- Keep artifact paths workspace-relative and schema-validated.
- Add new commands to both `contributes.commands` and `activationEvents`.
- Update tests when adding schema fields, migration-map statuses or new diagram event behavior.
- Keep test output such as `dist-test/` out of the packaged `.vsix`.

## Build

```sh
cd renovatio-workbench
npm run build --workspace @renovatio/diagram-canvas

cd ../renovatio-vscode-extension
npm install
npm test
npm run build
npm run package
```

`npm test` compiles the pure workbench modules into `dist-test/` and runs Node's built-in test runner against migration-map helpers, diagram model updates, contributed commands and JSON Schema fixture contracts. The fixture workspace lives under `test/fixtures/workspace-basic/` so agents can extend coverage without requiring a VS Code UI session.

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
