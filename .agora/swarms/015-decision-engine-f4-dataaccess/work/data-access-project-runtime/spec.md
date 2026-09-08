# F4 · project-backed data-access classifications

## Outcome

`GET /api/projects/{id}/data-accesses` must expose classifications produced from the
project's latest successful COBOL analysis, using the project's effective migration
profile. It must never manufacture a synthetic program or read another project's state.

## Binding decisions

- The source of truth is the latest persisted `COMPLETED` `analyze` job for the requested
  project. The analysis payload must retain enough normalized program/IO data to recreate
  the semantic programs required by `DataAccessClassifier`; if it does not, the analysis
  persistence boundary is extended rather than silently returning an empty result.
- No successful analysis means the controller's existing `404 NOT_FOUND` response with an
  empty classification payload. Unknown project IDs continue to use the same not-found
  behavior; no synthetic data is introduced.
- The effective profile is resolved only for the requested project; source strategy
  overrides are applied by `classifyFromPrograms`.
- Results are deterministic: stable program/source ordering and classifier IDs, with no
  LLM call or filesystem-wide scan during a read request.

## Required evidence

Focused service/API tests cover real classified data, project isolation, effective-profile
overrides, empty/no-analysis behavior, and malformed persisted payload handling. The
relevant Maven module tests and the characterization guardrail must pass.
