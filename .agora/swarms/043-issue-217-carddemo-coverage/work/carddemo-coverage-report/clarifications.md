---
schema: "agora/clarifications/v1"
swarm: "issue-217-carddemo-coverage"
work: "carddemo-coverage-report"
created-at: "2026-09-08T02:21:36.436324Z"
last-run-input-sha256: "1e8b1456d0681a244f9c9194a9d376698eeca4520322786d0c733ed245baa420"
last-run-question-count: 0
last-run-unanswered-count: 0
last-run-by: "project:owner"
last-run-at: "2026-09-08T02:23:12.481182Z"
---

# Clarifications for carddemo-coverage-report

| Question | Answer | Actor | Timestamp | Input SHA-256 |
| --- | --- | --- | --- | --- |
| ¿Qué repositorio upstream y commit exacto de AWS CardDemo deben fijarse en PROVENANCE.md para que el corpus sea reproducible? |  | project:owner | 2026-09-08T02:21:36.436324Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿El total esperado del corpus es de 44 programas COBOL (31 base + 3 DB2 + 8 IMS/DB2/MQ + 2 VSAM/MQ), excluyendo copybooks, BMS y JCL del pipeline por programa? | Sí; esos 44 archivos COBOL constituyen el universo descrito, mientras que copybooks, BMS y JCL sólo se incluyen como fuentes auxiliares o para clasificación. | project:owner | 2026-09-08T02:21:36.436324Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿Cuál es la regla determinista para elegir los tres programas batch más simples y desempatar candidatos con igual cobertura? |  | project:owner | 2026-09-08T02:21:36.436324Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿La aceptación de aggregate-report debe exigir también el recuento de programas que emiten Java, aunque el criterio resumido sólo mencione parseo y compilación? | Sí; R3 exige explícitamente N/total que parsean, M/total que emiten Java y K/total cuyo Java compila. | project:owner | 2026-09-08T02:21:36.436324Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿Qué campos, ordenamientos y normalización de mensajes de error se exigirán en carddemo-coverage.json para considerar que dos corridas equivalentes producen un JSON determinista? |  | project:owner | 2026-09-08T02:21:36.436324Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿Cuál es el inventario canónico y denominador exacto de programas COBOL que debe procesar la herramienta (incluyendo extensiones admitidas y tratamiento de duplicados o programas auxiliares)? |  | project:owner | 2026-09-08T02:22:15.937137Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿Qué regla exclusiva debe usarse para clasificar cada programa en batch puro, CICS online, DB2, IMS+MQ o VSAM+MQ cuando contiene tecnologías de más de un subsistema? |  | project:owner | 2026-09-08T02:22:15.937137Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿Cuál es la definición operativa de “constructo no soportado”: presencia léxica, ManualActionItem, comentario `Unhandled`, fallo de IR/emisión, o una combinación explícita de esas señales? |  | project:owner | 2026-09-08T02:22:15.937137Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿Cómo se ordenan los candidatos E2E para determinar de forma reproducible los tres “más simples” y resolver empates entre programas con igual cobertura? |  | project:owner | 2026-09-08T02:22:15.937137Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
| ¿Se permite modificar JavaGenerationService únicamente para exponer métricas ya calculadas, o el criterio no-engine-change exige que permanezca completamente intacto? |  | project:owner | 2026-09-08T02:22:15.937137Z | 9f761183193351fae428312a86e991f64056fb44d05cfab8b5821d9f1021195a |
