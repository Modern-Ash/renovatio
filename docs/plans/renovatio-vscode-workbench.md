# Renovatio VS Code Two-Way Workbench

Parent epic: #281. Implemented child tickets: #283, #284, #290, #291 and #292. This handoff guide closes the documentation ticket #293.

## Philosophy

The VS Code plugin is a local-first modernization workbench. Human evaluators, Renovatio developers and future agents should be able to inspect every intermediate artifact, understand what the backend or LLM proposed, approve changes explicitly and export evidence for review.

The extension should prefer transparent files under `.renovatio/` over hidden state. Backend integration is useful, but local artifacts remain the source of truth unless the user explicitly pulls or pushes through the sync workflow.

## Architecture

- `renovatio-vscode-extension/package.json` declares commands, views, activation events, menus, schemas, languages and custom editors.
- `src/extension.ts` wires command registration and services.
- `src/workspaceManifest.ts` owns `.renovatio/workspace.renovatio.json` creation, loading, formatting and validation.
- `src/backendControl.ts` owns backend health checks, LLM smoke tests and local/dev server controls.
- `src/migrationMap.ts` owns migration-map read/write, validation and diagnostics.
- `src/workbenchCore.ts` contains pure helper functions for migration-map indexing, hover formatting, hash comparison and stale-state classification.
- `src/navigation.ts` owns CodeLens and hover navigation between legacy and generated code.
- `src/changeSet.ts` owns change-set storage helpers.
- `src/generationWorkflow.ts` owns preview, approval, rejection, apply and reconciliation.
- `src/evidenceBundle.ts` owns evidence bundle export and latest-bundle helpers.
- `src/sync.ts` owns optional backend revision comparison, pull, push and conflict status.
- `src/model.ts` parses diagram artifacts and applies diagram edit events.

## User Flow

1. Install or launch the extension.
2. Open a COBOL workspace.
3. Run `Renovatio: Initialize Workspace`.
4. Configure backend and LLM through `Renovatio: Open Backend Settings` and `Renovatio: Configure LLM Model`.
5. Run `Renovatio: Run Backend And LLM Checks`.
6. Analyze source roots from the Discovery view.
7. Review diagrams and migration map.
8. Use COBOL/JCL CodeLens to open generated targets, evidence and diffs.
9. Preview a migration diff, approve or reject each change, then apply only approved changes.
10. Export an evidence bundle.
11. Optionally sync with backend using status, compare, pull and push commands.

## Artifact Contracts

`.renovatio/workspace.renovatio.json` stores project id, source roots, target roots, artifact paths, backend configuration, LLM configuration and sync settings.

`.renovatio/migration-map.renovatio.json` stores source-to-target traceability. Entries should include a stable id, kind, source location, target location, status, confidence, evidence and optional source/target hashes.

`.renovatio/changesets/*` stores proposed file changes. Each change records target path, entry ids, before/after content or diff metadata, hashes and approval state.

`.renovatio/evidence-bundles/*` stores reviewable handoff bundles. A bundle should include a manifest, workspace manifest, migration map, diagrams, change sets, evidence files, checksums and summary.

Diagram artifacts use `*.renovatio-domain.json` and `*.renovatio-arch.json`; the custom editor preserves layout edits in the JSON artifact.

## Backend And LLM

Backend configuration belongs in the workspace manifest and can be overridden by VS Code settings only as a fallback. The Backend view should show URL, environment, last health check, backend version and last error.

LLM configuration belongs in the manifest `llm` block. Agents should keep provider, model, fallback model, prompt profile, temperature, token limit and cache policy visible to the user before analysis or reverse engineering.

Restart/start/stop commands must stay safety gated:

- `backend.environment` must be `local` or `dev`.
- `backend.allowLocalProcessControl` must be `true`.
- The user must see and confirm the exact command.
- Staging and production manifests must not expose local process control.

## Two-Way Navigation

Source editors use CodeLens and hovers to open mapped target code, evidence, domain nodes and migration diffs. Target editors use CodeLens and hovers to open legacy source, evidence, manual-refinement actions and reconciliation.

Missing migration maps should remain quiet. Missing mapped files should offer the migration map as the repair entry point. Hash mismatches should produce stale-state warnings without overwriting files.

## Safety Model

- Local artifacts are the default source of truth.
- Backend sync is opt-in.
- Pulls create backups before replacing local content.
- Pushes use expected revisions.
- Diffs are previewed before apply.
- Apply writes only approved changes.
- Manual edits can be reconciled into target hashes.
- Evidence export records warnings and partial-bundle decisions.

## Agent Handoff

When adding a command, update `package.json` in both `contributes.commands` and `activationEvents`, then register the handler in `src/extension.ts`.

When adding a view, update `package.json` view contributions and implement the provider in `src/views.ts`. Keep empty states actionable and tied to actual commands.

When changing artifact shape, update the TypeScript type, schema file, examples, fixture workspace and tests. Prefer pure helpers under `src/workbenchCore.ts` when behavior can be tested without VS Code APIs.

When adding navigation behavior, update `src/navigation.ts` and cover pure indexing/hover behavior in `test/workbench-core.test.js`.

When changing diagram behavior, update `src/model.ts`, the shared canvas package if needed and `test/diagram-model.test.js`.

When changing backend, LLM, sync, change-set or evidence behavior, keep the README command names synchronized with `package.json` and add fixtures that show the expected artifact handoff.

## Verification

Run these before publishing a branch:

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

Use `vsce package --no-dependencies` output to confirm tests and `dist-test/` are not shipped inside the `.vsix`.
