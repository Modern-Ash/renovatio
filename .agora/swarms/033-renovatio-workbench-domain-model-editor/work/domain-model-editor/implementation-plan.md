# Implementation plan · DomainModel view and editor (#179)

## Boundary and dependency recovery

1. Reapply the three self-contained #169 commits that introduce
   `renovatio-domain-model`, `SemanticDomainProjector` and the serialized-analysis
   bridge. Do not import the unmerged dashboard, architecture projection or
   equivalence changes that followed them.
2. Keep schema v1 backward compatible while adding structured node properties
   and relation cardinalities through null-safe defaults and compatibility
   constructors. Extend contract tests for deterministic ordering, canonical
   hashes and validation failures.

## Backend · `renovatio-api`

3. Add immutable `ProjectDomainModelVersionEntity` snapshots keyed by project +
   revision, plus a suggestion-decision entity keyed by project + suggestion id.
   Add repositories with newest-first history and exact revision lookup.
4. Add `WorkbenchDomainModelDto` for the editor read model, version metadata,
   validation diagnostics, suggestion decisions and structured comparisons.
5. Add `WorkbenchDomainModelService` to:
   - return an explicit empty v1 model at revision zero;
   - validate project ownership, ids, properties, relations and invariants;
   - save only hash-changing snapshots with optimistic revision checks;
   - list, restore and compare immutable revisions;
   - derive undecided suggestions from LLM-origin nodes/invariants and record
     accept/edit/reject outcomes without automatic application;
   - reject deletion of referenced nodes and expose missing-evidence warnings.
6. Extend `WorkbenchProjectController` with read/save/history/restore/compare and
   suggestion-triage routes. Reuse `canView` for reads and the existing
   `ApiAccessService.canModify`/development bypass for mutations.
7. Provision the two tables for SQLite startup without enabling schema mutation,
   while retaining `create-drop` test-profile behavior.
8. Add focused service and MVC/controller tests for validation, canonical hash,
   optimistic conflict, immutable restore, diffs, authorization and triage.

## Frontend · `renovatio-core-ui`

9. Add a sixth `domain` activity area and command/keybinding. Load its adapter on
   project selection and expose all explicit states.
10. Build an industrial three-panel editor within the established Control Deck:
    - filterable element rail grouped by kind;
    - structured node/relation editor with live validation and dirty state;
    - provenance inspector with source navigation, confidence and hashes;
    - immutable version rail and structured compare summary;
    - governed LLM suggestion queue with explicit accept/edit/reject buttons.
11. Preserve accessibility with labels, `aria-current`, `aria-live`, visible
    focus, keyboard-sized targets, responsive collapse and reduced-motion rules.
12. Extend frontend contract tests and HTTP smoke bundle assertions for the new
    endpoints, states, navigation, optimistic save and absence of generation or
    architecture mutations.

## Verification and governance

13. Run module tests for `renovatio-domain-model`, focused API tests, full API
    test compilation, workbench unit/contract tests, workbench build and HTTP
    smoke where dependencies/environment permit.
14. Record `verification-report.md` and `review-report.md`, register successful
    evidence, advance every criterion through implemented/verified/accepted, and
    request the recorded Spec Owner approval only after all gates pass.

## Risk controls

- A stale save returns conflict and never overwrites history.
- Empty models remain empty; the adapter never invents business elements.
- Model edits never trigger source writes, analysis or code generation.
- Missing #169 dependency commits are carried explicitly and tested rather than
  replaced with a Workbench-only duplicate model.
