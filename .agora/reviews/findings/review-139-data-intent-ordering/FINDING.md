---
schema: "agora/review-finding/v1"
id: "review-139-data-intent-ordering"
swarm: "ai-modernization"
work: "annotated-openrewrite-pass"
pass: "pr-139-review"
severity: "high"
status: "resolved"
policy: "annotated-consumption"
location: "cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicator.java"
created-at: "2026-08-31T12:45:12.984554Z"
decided-by: "project:agent"
decided-at: "2026-08-31T12:50:24.488512Z"
decision-reason: "Commit 294e091 applies DATA_INTENT before DOMAIN_NAMING deterministically and covers both annotations on one node; 167/167 offline tests and the Java 17 network-disabled reactor lane pass"
---

# Review finding review-139-data-intent-ordering

## Summary

DATA_INTENT can be skipped when DOMAIN_NAMING renames the same field first
