---
schema: "agora/review-finding/v1"
id: "mutable-test-report-evidence"
swarm: "ai-modernization"
work: "residual-semantic-enrichment"
pass: "completion-review"
severity: "high"
status: "resolved"
policy: "evidence-integrity-v1"
location: "docs/reports/residual-semantic-enrichment-test-report.md"
created-at: "2026-08-31T00:44:27.885427Z"
decided-by: "project:agent"
decided-at: "2026-08-31T00:47:17.119363Z"
decision-reason: "Historical report restored at SHA-256 f3224cc6c753f65ed532c8abf66238794da74383dc0d122b06633a99ebe95def; revalidation preserved at versioned URI SHA-256 77fb27fa105b4d03d2193958ec86192cb7227983caed91c916b2b3a714eac545; evidence-000003/000004 references corrected without changing test facts; agora validate is ok."
---

# Review finding mutable-test-report-evidence

## Summary

Two historical successful evidence entries reference an earlier digest at the mutable test-report URI, so agora validate reports evidence-entry.artifact-changed after revalidation updated that file.
