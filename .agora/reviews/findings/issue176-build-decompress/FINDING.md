---
schema: "agora/review-finding/v1"
id: "issue176-build-decompress"
swarm: "renovatio-workbench"
work: "issue-176-theia-platform-spike"
pass: "platform-spike-technical-review"
severity: "high"
status: "waived"
policy: "environment-security"
location: "renovatio-workbench/package-lock.json"
created-at: "2026-09-06T02:14:17.756620Z"
decided-by: "project:owner"
decided-at: "2026-09-06T12:09:33.235327Z"
decision-reason: "Excepci\u00f3n expl\u00edcita y limitada del propietario para el advisory cr\u00edtico de decompress presente s\u00f3lo en el grafo de desarrollo de Theia CLI. La imagen runtime multi-stage lo excluye; no autoriza producci\u00f3n ni release."
---

# Review finding issue176-build-decompress

## Summary

Theia CLI retains development-only decompress 4.2.1 with critical advisories; multi-stage Docker contains it but production approval requires an upstream fix or explicit security control.
