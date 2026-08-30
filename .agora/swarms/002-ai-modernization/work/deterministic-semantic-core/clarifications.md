---
schema: "agora/clarifications/v1"
swarm: "ai-modernization"
work: "deterministic-semantic-core"
created-at: "2026-08-30T16:34:58.050146Z"
last-run-input-sha256: "d0d5722e3c7981f2276ed0a2a933504f01b2cc38271acc9a848513a1e1d0c937"
last-run-question-count: 0
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-08-30T16:41:39.824079Z"
---

# Clarifications for deterministic-semantic-core

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| What default scale-reduction policy must COMPUTE use without ROUNDED, including the required BigDecimal rounding mode or truncation behavior? | Truncate toward zero using `java.math.RoundingMode.DOWN`. | project:owner | 2026-08-30T16:36:29.536259Z | 37ce5642e2c16533a7a0777e304ad5cd903e7cb50bc5d793c83cf46adcb54dd7 |
| Which deterministic Java representation must v1 generate for level-88 conditions: an enum, a typed predicate/value object, or another specified construct? | Generate a typed predicate/value object with named condition methods; do not use an enum because values and ranges may overlap. | project:owner | 2026-08-30T16:36:29.536259Z | 37ce5642e2c16533a7a0777e304ad5cd903e7cb50bc5d793c83cf46adcb54dd7 |
| Does v1 support period-terminated nested IF scope, or require explicit END-IF for nested IF statements? | Require explicit `END-IF` for nested IF in v1; period-terminated nested scope is out of scope. | project:owner | 2026-08-30T16:36:29.536259Z | 37ce5642e2c16533a7a0777e304ad5cd903e7cb50bc5d793c83cf46adcb54dd7 |
| May implementation begin once the required issue #122 contracts are committed and available on the branch, or only after issue #122 reaches completed with its fixture corpus and offline CI lane green? | Implementation may begin from committed #122 contracts, but #123 cannot become verified until the required #122 characterization and offline gates are green. | project:owner | 2026-08-30T16:36:29.536259Z | 37ce5642e2c16533a7a0777e304ad5cd903e7cb50bc5d793c83cf46adcb54dd7 |
| Has `project:owner`, acting as Spec Owner, explicitly marked every open question resolved and approved the specification for the `spec-clarified` gate? | Yes. `project:owner` marks every open question resolved and approves the specification for the `spec-clarified` gate. | project:owner | 2026-08-30T16:41:12.238136Z | 87fb82ac69d8394622efc3e26e8f2b03168ead94d1de0cf7dcd2ddf2cd565e7f |
| Is `repo://docs/specs/deterministic-semantic-core.md` registered as the required `spec` artifact, rather than merely attached as specification content? | Yes. The Agora artifact ledger registers that URI with kind `spec`. | project:owner | 2026-08-30T16:41:12.238136Z | 87fb82ac69d8394622efc3e26e8f2b03168ead94d1de0cf7dcd2ddf2cd565e7f |
| Do the two identical specification entries with the same URI represent one canonical `spec` artifact? | Yes; they have identical content and the same URI. | project:owner | 2026-08-30T16:36:59.321126Z | 87fb82ac69d8394622efc3e26e8f2b03168ead94d1de0cf7dcd2ddf2cd565e7f |
| What exact issue #122 commit or branch reference must be present to satisfy the implementation dependency gate? | Commit `bbd35be` or a merge commit whose history includes it must be present. | project:owner | 2026-08-30T16:41:12.238136Z | 87fb82ac69d8394622efc3e26e8f2b03168ead94d1de0cf7dcd2ddf2cd565e7f |
| Must the `characterized` criterion include an explicit construct-to-fixture coverage matrix, or does the coverage list in section 7 fully define “every supported construct”? | Yes. The specification must include the explicit construct-to-test matrix in section 9.1. | project:owner | 2026-08-30T16:41:12.238136Z | 87fb82ac69d8394622efc3e26e8f2b03168ead94d1de0cf7dcd2ddf2cd565e7f |
