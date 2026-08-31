---
schema: "agora/review-finding/v1"
id: "review-139-domain-rename-idempotence"
swarm: "ai-modernization"
work: "annotated-openrewrite-pass"
pass: "pr-139-review"
severity: "medium"
status: "resolved"
policy: "ast-safe"
location: "cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicator.java"
created-at: "2026-08-31T12:45:12.704136Z"
decided-by: "project:agent"
decided-at: "2026-08-31T12:50:24.193927Z"
decision-reason: "Commit 294e091 adds repeated-application coverage and distinguishes original-plus-target collisions from an already-applied target; 167/167 offline tests and the Java 17 network-disabled reactor lane pass"
---

# Review finding review-139-domain-rename-idempotence

## Summary

Repeated OpenRewrite cycles classify an already-applied DOMAIN_NAMING rename as NAME_COLLISION
