---
schema: "agora/review-finding/v1"
id: "issue228-placeholder-stages"
swarm: "issue-228-cobol-java-reference-path"
work: "cobol-java-reference-path"
pass: "post-merge-ac07-audit"
severity: "critical"
status: "open"
policy: "end-to-end"
location: "renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/pipeline/CobolPipelineOrchestrator.java"
created-at: "2026-09-10T12:59:57.344590Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding issue228-placeholder-stages

## Summary

Pipeline reports parse, Semantic IR, decisions, manifest, and OpenRewrite success without executing those capabilities; Domain and Architecture stages are absent.
