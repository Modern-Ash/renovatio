---
schema: "agora/review-finding/v1"
id: "annotated-ir-review-005"
swarm: "ai-modernization"
work: "annotated-ir-contract"
pass: "github-review-pr-134"
severity: "critical"
status: "resolved"
policy: "acceptance.content-identity"
location: "renovatio-cobol-ir/src/main/java/org/shark/renovatio/cobol/ir/annotated/CanonicalJson.java"
created-at: "2026-08-30T18:10:44.963953Z"
decided-by: "project:agent"
decided-at: "2026-08-30T18:11:36.531826Z"
decision-reason: "Integral and BigInteger values now canonicalize exactly without double conversion; BigDecimal uses a lossless normalized representation; regression proves the two reported values produce distinct canonical bytes and hashes."
---

# Review finding annotated-ir-review-005

## Summary

Canonical JSON converted integral values through double and allowed distinct base IR documents above 2^53 to collide.
