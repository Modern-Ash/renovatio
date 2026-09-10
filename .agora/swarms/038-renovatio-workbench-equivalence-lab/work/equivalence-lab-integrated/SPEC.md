# Theia 8 · Equivalence Lab integrado

## Objective

Deliver GitHub issue #184 by making the Theia workbench execute and explain COBOL/target equivalence from inside the workbench, with reproducible runs, divergence triage, promotion gating, audited reports, and commit/profile/change-set history.

## Scope

- Expose fixtures with source, case, baseline, candidate, editable input fields, sequential files, DB2 responses, evidence references, and reproducibility hashes.
- Execute deterministic equivalence runs through governed backend endpoints.
- Support exact repeat, cancel, report export, and divergence triage actions.
- Block promotion when no completed run exists, when release-blocking verdicts exist, or when divergences remain unaccepted.
- Render the integrated lab in the Theia UI with selectors, inputs, async state, logs, comparisons, triage, gate, report export, and audit history.

## Out of scope

- Real host execution orchestration and long-running queue workers. This issue establishes the workbench contract and deterministic smoke/divergence fixture behavior for later runtime adapters.
