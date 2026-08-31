---
schema: "agora/review-finding/v1"
id: "review-139-java-literal-escaping"
swarm: "ai-modernization"
work: "annotated-openrewrite-pass"
pass: "pr-139-review"
severity: "high"
status: "open"
policy: "fallback"
location: "cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicator.java"
created-at: "2026-08-31T12:45:13.274451Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding review-139-java-literal-escaping

## Summary

Raw CR or LF in accepted annotation strings can make JavaTemplate parsing fail
