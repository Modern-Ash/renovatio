# Issue 233 Implementation Plan

## Phase 1: Decouple batch suggestions from the LLM runtime

- Add a stable `DecisionSuggestionPort` to `renovatio-decisions`.
- Make `DecisionSuggestionService` implement that port from `renovatio-llm`.
- Change `BatchSuggestionAdapter` to depend on `DecisionSuggestionPort`.
- Remove the `renovatio-llm` dependency from `renovatio-jcl/pom.xml`.
- Add or update tests so JCL references prompt identity through the common port.

## Phase 2: Canonical application preview hook

- Add `BatchOrchestrationPlanner` to the application ports.
- Keep the default planner inert.
- Invoke the planner in `DefaultRenovatioApplication.preview` after target
  emission and before refinement/manifest creation.
- Cover the hook with an application contract test that proves batch artifacts
  become part of the same manifest used by validate/apply.

## Phase 3: Verification and evidence

- Run the focused tests for `renovatio-decisions`, `renovatio-llm`,
  `renovatio-jcl` and `renovatio-application`.
- Run `git diff --check`.
- Record results in `docs/reports/issue-233-jcl-integration-report.md` and
  `docs/reports/issue-233-test-report.md`.
