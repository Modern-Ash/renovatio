# F8 post-merge review corrections — implementation plan

1. Add decision-policy regressions for local confirmations and option-vocabulary
   drift, then adjust policy candidate selection and stale reporting without
   weakening the auto-confirm threshold.
2. Add an exact legacy-vs-layered hash regression for an unbound project, then omit
   empty binding metadata from the canonical effective-profile projection.
3. Extend the reusable CLI store with local effective-profile resolution and
   deterministic F1 decision reconciliation. Install it as the CLI's
   `EffectiveProfileResolver`, carry the original project identity through plan
   replay/apply, and materialize the effective template on `profile apply`.
4. Add `decisions list/set`; make successful analysis persist/reconcile decisions;
   exercise an end-to-end CLI flow that analyzes, confirms, exports, and reuses a
   policy without direct store seeding.
5. Run focused module tests, full reactor tests, characterization tests, UI build,
   and whitespace validation. Record verification and review artifacts before
   requesting Spec Owner acceptance.

## Criterion coverage

- `local-confirmation-precedence`: step 1.
- `legacy-hash-compatibility`: step 2.
- `cli-profile-runtime`: step 3 plus CLI integration tests.
- `cli-policy-export-runtime`: step 4 plus CLI integration tests.
- `stale-policy-signaling`: step 1.
- `regression-quality`: step 5.
