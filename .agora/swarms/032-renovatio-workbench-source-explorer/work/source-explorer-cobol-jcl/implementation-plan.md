# Implementation plan · Source Explorer COBOL/JCL/Copybooks (issue #178)

## Boundary

Read-only increment on the `agora/renovatio-workbench` line, following the
established "Workbench area wired to a governed backend contract" pattern
(swarms 023–029). No change to backend analysis flows, no writes, no identity/login.

## Backend — `renovatio-api`

1. `dto/WorkbenchSourceExplorerDto.java` — records:
   `SourceExplorerFile(id, name, kind, path, hash, encoding, analysisStatus, List<Symbol> symbols, List<Diagnostic> diagnostics)`,
   `Symbol(id, kind, name, line, column, parentId, irCoordinate)`,
   `Diagnostic(severity, message, line)`,
   `Dataset(id, name, List<String> referencedBy)`,
   top-level `WorkbenchSourceExplorerDto(List<SourceExplorerFile> files, List<Dataset> datasets)`.
2. `service/WorkbenchSourceExplorerService.java` — walks the project workspace
   root (reusing `ProjectService` + `Files.walk`, depth 8, same as
   `WorkbenchProjectAdapterService`). For each `.cbl/.cob`, `.cpy`, `.jcl`:
   - compute SHA-256 (`MessageDigest`), detect encoding
     (`US-ASCII` when all bytes < 0x80, else `UTF-8`),
   - line-scan for structural symbols:
     - COBOL: `IDENTIFICATION/ENVIRONMENT/DATA/PROCEDURE DIVISION`, `... SECTION.`,
       paragraph headers (`^ {0,4}[A-Z0-9][A-Z0-9-]*\.` in procedure division),
       `PERFORM <name>`, `CALL '<name>'`, `EXEC SQL … END-EXEC`,
       `EXEC CICS … END-EXEC`, `COPY <name>`.
     - JCL: `//name EXEC …` steps, `//name DD …` statements, `DSN=<dataset>`.
   - `analysisStatus`: `parsed` when a program id / at least one division is found;
     `partial` when the file has recognizable COBOL but no clean division set;
     `unsupported` for any other extension reaching the walker.
   - diagnostics: `COPY <x>` with no matching `<x>.cpy` in the workspace →
     `warning`; a `PROCEDURE DIVISION` with zero paragraphs → `info`.
   - `irCoordinate`: deterministic `ir://<program>/<kind>/<name>` string; it is a
     stable reference, not a live IR fetch.
   - datasets: aggregated from JCL `DSN=` values with their referencing members.
   Results are sorted deterministically (path, then line).
3. `controller/WorkbenchProjectController.java` — add
   `GET /workbench/source-explorer` guarded by the existing `canView(role)` check,
   delegating to the service. No new CORS or auth surface.

## Frontend — `extensions/renovatio-core-ui`

4. `renovatio-shell-widget.tsx` — within the existing **Project** area add a
   "Source explorer" panel:
   - fetch `/api/projects/{id}/workbench/source-explorer` on project load and
     project switch, with `loading | ready | empty | permission-denied | error`
     states mirroring the other areas;
   - render the file tree grouped by `kind`, each file showing
     `hash · encoding · analysisStatus`;
   - an **Outline** list for the selected file (symbols with `kind`, `name`,
     `line:column`), keyboard operable, `aria-current` on the active symbol;
   - a **search** input filtering symbols/text across files, resolving hits to
     `file line:column`;
   - a **Problems** list rendering `diagnostics` for the selected file;
   - selecting a symbol shows its `irCoordinate` (IR linkage) in the inspector.
5. `style/renovatio-workbench.css` — layout for the tree/outline/problems columns,
   reusing existing tokens, focus-visible and reduced-motion rules.

## Tests / evidence

6. `renovatio-api` — `WorkbenchSourceExplorerServiceTest` (JUnit): fixture
   workspace with one COBOL program (divisions, sections, paragraphs, PERFORM,
   CALL, EXEC SQL, EXEC CICS, COPY present + COPY missing), one copybook, one JCL
   with two steps and a `DSN=`, and one unsupported `.txt`. Asserts symbol kinds,
   positions, hash/encoding/status, the missing-copybook diagnostic, dataset
   aggregation, and that an unsupported file yields an empty symbol list.
7. `renovatio-core-ui` — extend `tests/contract.test.mjs`: assert the widget
   calls `/workbench/source-explorer`, renders outline/search/Problems, exposes
   `sourceExplorerState` with all five states, keeps `aria-current` on symbols,
   and never issues a mutating request to the endpoint.
8. `scripts/http-smoke.mjs` — extend to assert the built bundle references
   `workbench/source-explorer`.
9. Artifacts: `verification-report.md` (test output summary), `review-report.md`
   (gate decision), evidence record `type=integration`.

## Out of scope (documented follow-ups)

- Live IR document rendering and cross-file "go to definition".
- Copybook expansion/inlining view.
- Monaco-based COBOL syntax highlighting (kept as a shell follow-up).
- macOS interactive capture (same platform constraint as #176/#177).
