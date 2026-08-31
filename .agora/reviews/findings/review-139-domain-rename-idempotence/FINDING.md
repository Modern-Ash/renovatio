---
schema: "agora/review-finding/v1"
id: "review-139-domain-rename-idempotence"
swarm: "ai-modernization"
work: "annotated-openrewrite-pass"
pass: "pr-139-review"
severity: "medium"
status: "open"
policy: "ast-safe"
location: "cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicator.java"
created-at: "2026-08-31T12:45:12.704136Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding review-139-domain-rename-idempotence

## Summary

Repeated OpenRewrite cycles classify an already-applied DOMAIN_NAMING rename as NAME_COLLISION
