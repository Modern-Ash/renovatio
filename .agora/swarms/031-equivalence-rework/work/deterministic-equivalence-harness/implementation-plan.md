# Implementation plan

1. Define `EquivalenceRunner`, fixture observations and the four-way classification.
2. Provide file and command adapters that capture output hashes, exit codes and invariants.
3. Add deterministic record, numeric and business-rule comparators.
4. Persist reports as canonical JSON containing both sides' invariants and `blocksRelease`.
5. Integrate persisted reports into the Workbench Equivalence adapter and UI.
6. Verify with unit tests, the COBOL/Java `equivalence-balance` golden fixture, API endpoint smoke,
   and the workbench contract test.
7. Document readiness, shadow/canary boundaries and rollback; defer production execution pending an
   approved operational environment.
