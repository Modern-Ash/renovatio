---
schema: "agora/review-finding/v1"
id: "issue228-surface-not-pipeline"
swarm: "issue-228-cobol-java-reference-path"
work: "cobol-java-reference-path"
pass: "post-merge-ac07-audit"
severity: "high"
status: "resolved"
policy: "surface-proof"
location: "renovatio-provider-cobol/src/test/java/org/shark/renovatio/provider/cobol/pipeline/SurfaceProofTest.java"
created-at: "2026-09-10T12:59:57.971239Z"
decided-by: "project:agent"
decided-at: "2026-09-10T14:06:30.671376Z"
decision-reason: "Service and MCP adapters delegate to the same orchestrator; surface proof executes both paths."
---

# Review finding issue228-surface-not-pipeline

## Summary

SurfaceProofTest compares analysis and helper endpoints, not execution of the complete reference migration through the same application and MCP boundary.
