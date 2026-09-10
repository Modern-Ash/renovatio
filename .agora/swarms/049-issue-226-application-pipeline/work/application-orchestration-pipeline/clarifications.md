---
schema: "agora/clarifications/v1"
swarm: "issue-226-application-pipeline"
work: "application-orchestration-pipeline"
created-at: "2026-09-09T21:10:08.555849Z"
last-run-input-sha256: "a80599681b63e9e42fd054856b3a726710091cc16e45bca4cd0e75019d8915d6"
last-run-question-count: 0
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-09-09T21:19:01.801412Z"
---

# Clarifications for application-orchestration-pipeline

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| ¿Cuál es el límite transaccional obligatorio de apply cuando intervienen filesystem, persistencia y Git? | El commit atómico abarca el conjunto de artefactos del workspace y el registro durable de ChangeSet/idempotencia. Todo se prepara antes de publicar; si falla una escritura se restaura el preimage y no se publica éxito. Un commit Git local, cuando corresponda, ocurre sólo después de materializar y validar el conjunto y debe poder compensarse; red y pushes remotos quedan fuera de AC-05 y no forman parte de apply. | project:owner | 2026-09-09T21:10:08.555849Z | d486eef00a5d7b86122c174e62fc3a48288ff87475912ee959ffa8547b19734a |
| ¿Hasta dónde debe migrar AC-05 las superficies API, CLI y MCP sin invadir AC-09? | AC-05 migra únicamente las rutas productivas de plan, preview, validate y apply para que deleguen en los mismos casos de uso y errores tipados. AC-09 conserva la unificación completa de catálogo de capabilities, discovery, UX y paridad de todas las operaciones. | project:owner | 2026-09-09T21:10:08.555849Z | d486eef00a5d7b86122c174e62fc3a48288ff87475912ee959ffa8547b19734a |
| ¿Puede mantenerse la orquestación legacy durante la transición? | Sólo como adaptador temporal detrás de los nuevos puertos y con una prueba que impida que sea una segunda autoridad. No se admite dual routing productivo para una misma operación; cada superficie migrada debe tener una única ruta application y el retiro restante pertenece a AC-06. | project:owner | 2026-09-09T21:10:08.555849Z | d486eef00a5d7b86122c174e62fc3a48288ff87475912ee959ffa8547b19734a |
| ¿Qué semántica durable debe tener la idempotency key? | Se identifica por proyecto, caso de uso y clave; persiste un digest canónico del comando y su resultado terminal. Repetir clave y digest devuelve el resultado previo sin efectos; repetir la clave con otro digest falla con un error tipado. Los intentos fallidos o revertidos se registran sin hacerse pasar por éxito. | project:owner | 2026-09-09T21:10:08.555849Z | d486eef00a5d7b86122c174e62fc3a48288ff87475912ee959ffa8547b19734a |
| ¿Los nueve casos de uso enumerados deben implementarse en este ciclo o basta definir sus interfaces? | Los nueve deben existir como contratos públicos de application. Plan, preview, validate y apply deben quedar conectados end-to-end en las superficies en alcance; create/analyze, review domain, resolve decisions y export evidence pueden adaptar servicios existentes, pero también deben tener tests de contrato y no dejar decisiones de orquestación en los transportes. | project:owner | 2026-09-09T21:10:08.555849Z | d486eef00a5d7b86122c174e62fc3a48288ff87475912ee959ffa8547b19734a |
