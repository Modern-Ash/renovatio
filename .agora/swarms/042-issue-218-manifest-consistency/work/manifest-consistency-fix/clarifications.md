---
schema: "agora/clarifications/v1"
swarm: "issue-218-manifest-consistency"
work: "manifest-consistency-fix"
created-at: "2026-09-08T02:05:59.377145Z"
last-run-input-sha256: "e805907de4da666e5a9cfc2542a4ab6400172a44322b061883448db3e3eedb8b"
last-run-question-count: 3
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-09-08T02:05:59.377145Z"
---

# Clarifications for manifest-consistency-fix

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| ¿La paridad entre previewArchitecture y generateInterfaceStubs debe comparar también el orden de las rutas, además de su conjunto? | Sí; el objetivo de la spec exige mismas rutas y mismo orden. | project:owner | 2026-09-08T02:05:59.377145Z | e805907de4da666e5a9cfc2542a4ab6400172a44322b061883448db3e3eedb8b |
| ¿El criterio canvas-package-roots-parity debe ampliarse formalmente para incluir suffixes, class names y coherencia de package/import, como exige R2? | Sí. R2 ya lo cubre: `architecture.package.*`, `architecture.suffix.*` y `architecture.class.*` se respetan en preview y generación, y el `package`/`import` del código generado es coherente con esos roots. | project:owner | 2026-09-08T02:05:59.377145Z | e805907de4da666e5a9cfc2542a4ab6400172a44322b061883448db3e3eedb8b |
| ¿El criterio regression-green incluye también ejecutar y aprobar las suites de renovatio-architecture y renovatio-provider-java, además del comando de renovatio-provider-cobol indicado en el criterio actual? | Sí. R4 lo exige explícitamente: además de `renovatio-provider-cobol`, quedan verdes `renovatio-architecture` y `renovatio-provider-java`. | project:owner | 2026-09-08T02:05:59.377145Z | e805907de4da666e5a9cfc2542a4ab6400172a44322b061883448db3e3eedb8b |
