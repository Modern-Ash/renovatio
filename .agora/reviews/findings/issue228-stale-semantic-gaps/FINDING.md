---
schema: "agora/review-finding/v1"
id: "issue228-stale-semantic-gaps"
swarm: "issue-228-cobol-java-reference-path"
work: "cobol-java-reference-path"
pass: "post-merge-ac07-audit"
severity: "critical"
status: "resolved"
policy: "semantic-gaps"
location: "renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/pipeline/CobolPipelineOrchestrator.java"
created-at: "2026-09-10T12:59:57.757043Z"
decided-by: "project:agent"
decided-at: "2026-09-10T14:06:30.433498Z"
decision-reason: "SemanticGapTracker is wired into generation and emits stable warning/blocking action items."
---

# Review finding issue228-stale-semantic-gaps

## Summary

Stale-source detection and SemanticGapTracker are not connected to pipeline execution, so unsupported or changed input cannot produce the required blocking action items.
