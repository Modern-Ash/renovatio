---
schema: "agora/review-finding/v1"
id: "issue176-build-decompress"
swarm: "renovatio-workbench"
work: "issue-176-theia-platform-spike"
pass: "platform-spike-technical-review"
severity: "high"
status: "open"
policy: "environment-security"
location: "renovatio-workbench/package-lock.json"
created-at: "2026-09-06T02:14:17.756620Z"
decided-by: null
decided-at: null
decision-reason: null
---

# Review finding issue176-build-decompress

## Summary

Theia CLI retains development-only decompress 4.2.1 with critical advisories; multi-stage Docker contains it but production approval requires an upstream fix or explicit security control.
