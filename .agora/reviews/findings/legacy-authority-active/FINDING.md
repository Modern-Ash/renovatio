---
schema: "agora/review-finding/v1"
id: "legacy-authority-active"
swarm: "architecture-convergence-2026"
work: "canonical-domain-architecture-model"
pass: "gap-audit-2026-09-09"
severity: "high"
status: "resolved"
policy: "acceptance:legacy-retirement"
location: "renovatio-architecture/src/main/java/org/shark/renovatio/architecture/ArchitectureTransformer.java:17"
created-at: "2026-09-09T19:05:48.601109Z"
decided-by: "project:owner"
decided-at: "2026-09-09T20:22:14.189292Z"
decision-reason: "Resolved by commit f1e4f02: canonical projection is integrated, tested, documented, and governed"
---

# Review finding legacy-authority-active

## Summary

ArchitectureTransformer and ArchitectureGraph remain active public production authorities and are neither internalized nor deprecated for removal.
