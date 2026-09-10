---
schema: "agora/review-finding/v1"
id: "preview-apply-not-canonical"
swarm: "architecture-convergence-2026"
work: "canonical-domain-architecture-model"
pass: "gap-audit-2026-09-09"
severity: "high"
status: "resolved"
policy: "acceptance:preview-apply"
location: "renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/JavaGenerationService.java:179"
created-at: "2026-09-09T19:05:48.288303Z"
decided-by: "project:owner"
decided-at: "2026-09-09T20:22:13.973637Z"
decision-reason: "Resolved by commit f1e4f02: canonical projection is integrated, tested, documented, and governed"
---

# Review finding preview-apply-not-canonical

## Summary

Preview and generation share ArchitectureTransformer output, but neither consumes DomainModel plus DecisionSet nor a persisted content-addressed canonical manifest.
