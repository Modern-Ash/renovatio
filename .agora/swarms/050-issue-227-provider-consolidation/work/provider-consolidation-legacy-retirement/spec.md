# Issue 227 corrective specification

## Provenance

This record is retrospective. PR #247 and GitHub issue #227 were merged and closed before the durable Agora work record was recovered. The original architecture-convergence swarm was already terminal, so this independent corrective swarm records the audit and remediation without rewriting history.

## Required outcome

Provider selection must have one fail-closed authority. COBOL must not depend on the Java provider or Core/web implementation packages. Target emitters must be selected through a shared registry; source providers must not instantiate JavaEmitter or NodeEmitter. The legacy routing switch and legacy-named generation method must be absent. Production diagnostics must use structured logging. Existing Java and Node generation contracts must remain covered by characterization tests.

## Acceptance

The seven criteria in WORK.md are binding. Verification requires the relevant Maven reactor tests, active ArchUnit dependency rules, and source/POM scans for forbidden dependencies, legacy route names, direct emitter construction, and production System streams.
