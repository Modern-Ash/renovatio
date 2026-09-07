# Verification report

Date: 2026-09-07  
Actor: `project:agent` (`developer`)

## Result

The issue #179 implementation passes the model, persistence, API, frontend
contract and production Theia build gates. The editor does not call generation,
analysis or architecture mutation endpoints.

## Executed checks

| Check | Result | Coverage |
| --- | --- | --- |
| `mvn -q test -Dexec.skip=true` | Pass: 626 tests, 0 failures, 0 errors | Full Java reactor regression before the final deterministic evidence-ordering hardening |
| `mvn -q -pl renovatio-domain-model -am test -Dtest=DomainModelTest -Dsurefire.failIfNoSpecifiedTests=false -Dexec.skip=true` | Pass: 6 DomainModel tests | Final model shape, validation, stable hash and provenance ordering |
| `mvn -q -pl renovatio-api -am test -Dtest=DomainModelTest,WorkbenchDomainModelServiceTest,WorkbenchDomainModelApiTest -Dsurefire.failIfNoSpecifiedTests=false -Dexec.skip=true` | Pass | Immutable persistence, optimistic conflict, diff, restore, suggestion decisions, authorization, invalid relation rejection and edit/save/reload round trip |
| `npm test` in `renovatio-workbench` | Pass: 15 contract tests | Six workbench areas, structured editor, states, API boundaries, accessibility and bidirectional source linkage |
| `docker build --target build -t renovatio-workbench-issue179-verify .` in `renovatio-workbench` | Pass | Exact Node 24.20.0/npm 11.19.0 install; TypeScript and Theia production browser/node builds finish with zero errors |
| `git diff --check` | Pass | No whitespace errors |

The API integration smoke performs a real edit through MockMvc, persists and
reloads the model through JPA/H2, compares two immutable revisions, restores a
historical revision as a new revision and records an AI suggestion decision.

## Acceptance trace

- `element-views`, `property-editor`: `renovatio-shell-widget.tsx` renders the
  three-panel catalog and labelled structured controls for nodes, properties,
  relations, cardinalities and invariants.
- `provenance-inspector`: source reference, rationale/provenance, source kind,
  encoding, hash and confidence are visible; missing evidence is a live draft
  warning.
- `bidirectional-navigation`: evidence opens the matching Source Explorer file;
  source files and symbols resolve matching DomainModel nodes without analysis.
- `versioning-compare`: API and UI expose version list, structured diff and
  restore-as-new-revision.
- `ai-suggestion-triage`: accepted, edited and rejected decisions are persisted
  with revision and timestamp; no suggestion is automatically applied.
- `validation-guards`: the immutable model and save service reject broken
  references and duplicate/invalid structure; the frontend disables save on
  local errors.
- `preview-live-neutral`: draft edits update the catalog immediately; the save
  path only writes neutral DomainModel snapshots and contains no file generation
  or architecture selection.

## Environment notes

The host Node version is 20, so the repository Dockerfile was used for the
pinned Node 24 build. The in-app browser service reported no available browser,
therefore interactive screenshot QA was not possible in this run. The compiled
UI contract and API edit round trip are recorded; CI should still execute the
existing HTTP smoke on the published branch.

`npm ci` reported dependency advisories already present in the unchanged
lockfile (the production prune reported 22 moderate advisories). This change
adds no npm dependency or lockfile modification.
