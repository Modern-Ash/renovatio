---
schema: "agora/review-finding/v1"
id: "issue228-validator-evidence"
swarm: "issue-228-cobol-java-reference-path"
work: "cobol-java-reference-path"
pass: "post-merge-ac07-audit"
severity: "high"
status: "resolved"
policy: "determinism-idempotency"
location: "renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/pipeline/DeterminismValidator.java"
created-at: "2026-09-10T12:59:57.550216Z"
decided-by: "project:agent"
decided-at: "2026-09-10T14:06:30.204773Z"
decision-reason: "Production determinism and idempotency tests exercise generated-tree snapshots and stale-source rejection."
---

# Review finding issue228-validator-evidence

## Summary

Determinism and idempotency validators compare stage messages instead of independent generated file trees and have no focused test coverage.
