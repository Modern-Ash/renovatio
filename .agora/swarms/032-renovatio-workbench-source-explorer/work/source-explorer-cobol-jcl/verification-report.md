# Verification report · Source Explorer COBOL/JCL/Copybooks (issue #178)

## Adapter

`GET /api/projects/{projectId}/workbench/source-explorer` is served by
`WorkbenchProjectController.sourceExplorer(...)`, guarded by the existing
`canView(role)` boundary, delegating to `WorkbenchSourceExplorerService`. The
service only reads files inside the project's normalized workspace root; it
issues no write and never invokes the semantic pipeline.

## Backend tests — `renovatio-api`

`mvn -pl renovatio-api test -Dtest='Workbench*'` → **11 passed, 0 failed**.

`WorkbenchSourceExplorerServiceTest` (5 cases) proves:

| Criterion | Evidence |
| --- | --- |
| navigable-tree | `buildsNavigableProgramTreeWithPositionedSymbols` — divisions, `MAIN-SECTION`, and paragraph `0100-MAIN` with `parentId=PAYROLL#section:MAIN-SECTION`. |
| symbol-outline | same test — `perform`, `call`, `exec-sql`, `exec-cics`, `copy` symbols each with `line`/`column`; JCL steps/DD in `aggregatesJclStepsDdStatementsAndDatasets`. |
| file-metadata | `program.hash` starts `sha256:`, `encoding=US-ASCII`, `analysisStatus=parsed`; copybook resolves to `partial`. |
| ir-diagnostic-linkage | `paragraph.irCoordinate == ir://PAYROLL/paragraph/0100-MAIN`; `reportsMissingCopybookAsDiagnosticButKeepsFileNavigable` asserts the `COPY MISSINGCPY` warning and that a present copybook produces none. |
| resilient-unsupported | `unsupportedFilesAreExcludedSoWorkspaceStaysNavigable` — `NOTES.TXT` absent, other three files still returned with non-null symbol lists; `copybookFileParsesWithoutProcedureDivision`. |
| review-boundary | service exposes only `explore`/`summary` read methods; controller adds a single `@GetMapping`. |

## Frontend tests — `renovatio-core-ui`

`npm run build` (tsc `-b`) → **0 errors**.
`npm test` → **12 passed, 0 failed**, including
`exposes a read-only Source Explorer over the governed adapter (issue #178)`:
asserts the widget calls `/workbench/source-explorer`, exposes
`sourceExplorerState` in all five states, renders `Source files` tree,
`Outline of ${file.name}`, `Search symbols and references`, `Problems`,
`symbol.irCoordinate`, `aria-current` on the active file, surfaces
`hash`/`encoding`/`analysisStatus`, and never issues a mutating request to the
endpoint.

## Application bundle

`npm run build:application` (`theia build --mode production`) →
`[build/browser] Finished with 0 errors`, `[build/node] Finished with 0 errors`.
`applications/browser/lib/frontend/bundle.js` contains `workbench/source-explorer`.

## Not verified here (documented follow-ups)

- Interactive browser capture of the Source Explorer panel and the macOS run —
  same platform constraint recorded for issues #176 and #177; requires the
  spec owner's Node 24 environment.
- Live IR document rendering / cross-file go-to-definition.
- Monaco COBOL syntax highlighting.
