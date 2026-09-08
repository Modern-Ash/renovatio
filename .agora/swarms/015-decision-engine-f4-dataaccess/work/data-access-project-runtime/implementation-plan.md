# Implementation plan · project-backed data-access classifications

1. Add a project-scoped analysis snapshot abstraction backed by the latest completed
   `analyze` job, with explicit project/operation/status filtering and safe JSON parsing.
2. Preserve a normalized, classifier-ready representation of analyzed semantic programs at
   the analysis boundary; do not re-scan the filesystem from a read request and do not use
   synthetic programs.
3. Resolve the effective profile for the requested project and delegate classification and
   source-strategy overrides to `DataAccessService.classifyFromPrograms`.
4. Define deterministic empty and malformed-snapshot behavior, plus API/service tests for
   isolation, profile overrides, and no-analysis cases.
5. Run focused tests, Maven regression, characterization guardrail, and diff hygiene; record
   verification and review artifacts before completion.
