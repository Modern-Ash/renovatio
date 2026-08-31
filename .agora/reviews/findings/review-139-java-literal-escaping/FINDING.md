---
schema: "agora/review-finding/v1"
id: "review-139-java-literal-escaping"
swarm: "ai-modernization"
work: "annotated-openrewrite-pass"
pass: "pr-139-review"
severity: "high"
status: "resolved"
policy: "fallback"
location: "cobol-openrewrite-recipes/src/main/java/org/shark/renovatio/cobol/recipes/annotate/AnnotationApplicator.java"
created-at: "2026-08-31T12:45:13.274451Z"
decided-by: "project:agent"
decided-at: "2026-08-31T12:50:24.781917Z"
decision-reason: "Commit 294e091 escapes Java control characters and covers CR, LF, tab, quotes, and backslashes; 167/167 offline tests and the Java 17 network-disabled reactor lane pass"
---

# Review finding review-139-java-literal-escaping

## Summary

Raw CR or LF in accepted annotation strings can make JavaTemplate parsing fail
