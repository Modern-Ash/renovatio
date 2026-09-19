---
schema: "agora/work/v1"
id: "domain-entities-eval-harness"
swarm: "llm-domain-entities-epic"
title: "Eval harness: precisi\u00f3n/recall LLM-asistido vs determinista sobre CardDemo"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"xref-detected":"XREF-FILE -> ACCTFILE-FILE (o ACCOUNT post-merge) es detectada en modo LLM-asistido","no-false-positives":"Los 4 falsos positivos conocidos no reaparecen en ning\u00fan modo","cost-reported":"Costo/latencia de la corrida LLM-asistida queda reportado","repeatable":"Harness ejecutable con un comando documentado"}
satisfied-criteria: []
criterion-statuses: {"xref-detected":[],"no-false-positives":[],"cost-reported":[],"repeatable":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-domain-entities-epic/epic-llm-domain-entities"
---

# Eval harness: precisión/recall LLM-asistido vs determinista sobre CardDemo

## Description

Parte de epic-llm-domain-entities. Depende de issue #4 (coordinator) y del "modo auto-review" de issue #5. Es el gate final del epic antes de habilitar el flujo LLM-asistido por defecto.

## Objetivo
Construir un eval harness en `renovatio-evals` que compare, sobre el proyecto real AWS CardDemo, el modo determinista-puro (código actual de `SemanticDomainProjector`) contra el modo LLM-asistido (coordinator de la issue #4 + auto-review de la issue #5), midiendo precisión/recall contra un ground-truth manual, y reportando costo/latencia.

## Contexto técnico necesario
- Módulo: `renovatio-evals` (revisar su estructura actual — `EvaluationTest.java` visto en el listado de archivos de `renovatio-llm`, pero confirmar si `renovatio-evals` tiene su propio harness o reutiliza clases de `renovatio-llm`).
- **Ground-truth conocido de esta sesión de trabajo** (para usar como casos base, ampliar con más si se identifican durante la implementación):
  - Relación real esperada: `XREF-FILE → ACCTFILE-FILE`, evidenciada por `MOVE XREF-ACCT-ID TO FD-ACCT-ID` en `CBTRN02C.cbl` línea 394 (`XREF-ACCT-ID` es campo de `CARD-XREF-RECORD`, definido en el copybook `CVACT03Y.cpy`; `FD-ACCT-ID` es el `RECORD KEY` de `ACCTFILE-FILE` en `CBACT01C.cbl` línea 32/54). Ver también variantes del mismo patrón en `CBACT04C.cbl` líneas 202/204 (`MOVE TRANCAT-ACCT-ID TO FD-ACCT-ID` / `TO FD-XREF-ACCT-ID`) y `CBTRN01C.cbl` líneas 175/242.
  - Falsos positivos conocidos y ya corregidos deterministamente (para verificar que el modo LLM-asistido tampoco los reintroduce): `ACCT-FILE`/`ACCOUNT-FILE`/`ACCTFILE-FILE` son la MISMA entidad física (los tres `ASSIGN TO ACCTFILE`, confirmado vía grep en `CBACT01C.cbl:29`, `CBACT04C.cbl:41`, `CBTRN01C.cbl:52`, `CBSTM03B.CBL:49`, `CBTRN02C.cbl:51`) — no deben aparecer como relación `ASSOCIATES_WITH` entre sí, deben colapsar en una sola entidad `ACCOUNT`. Mismo patrón con `CUST-FILE`/`CUSTOMER-FILE`.
  - Proyecto ya cargado en el backend local para pruebas: project_id `873d7a49-b178-414c-a10c-f5364ff37f3f` ("AWS CardDemo") en `renovatio-api/data/renovatio.db`, workspace en `/home/faguero/.renovatio/workspaces/aws-mainframe-modernization-carddemo/`. **Nota operacional importante**: el backend persiste el `DomainModel` por proyecto con semántica de MERGE, no de reemplazo (`JobService.seedDomainModel`/`mergeProjectedDeterministicModel`) — para verificar un cambio de código hay que borrar la fila de `project_domain_model_versions` de ese proyecto en la base SQLite antes de re-analizar, o el resultado viejo persiste silenciosamente.
- Revisar `BudgetEnforcer` (`renovatio-llm`) para poder capturar/reportar costo real de las invocaciones del eval, y `ContentAddressedCache` para entender si el eval debe forzar cache-miss (medir costo real) o puede aprovechar cache (medir costo de "segunda corrida").

## Tareas
1. Formalizar el ground-truth de CardDemo como un fixture de test (lista de relaciones esperadas + lista de falsos positivos que NO deben aparecer), basado en los casos ya documentados arriba, revisando el código fuente real de CardDemo para confirmar/ampliar antes de darlos por sentados.
2. Implementar el harness que corre ambos modos (determinista-puro vs. LLM-asistido con auto-review) sobre el mismo proyecto y calcula precisión/recall de relaciones `ASSOCIATES_WITH` contra el ground-truth.
3. Capturar y reportar costo/latencia de la corrida LLM-asistida (usando `BudgetEnforcer`/logging existente), extrapolando a portfolios más grandes (44 programas en CardDemo — estimar costo para un proyecto de 200+ programas, escala típica de un mainframe real).
4. Definir el criterio de "listo para habilitar por defecto" (ej. recall ≥ X% sin nuevos falsos positivos, costo por proyecto por debajo de un umbral) y documentarlo — esta decisión probablemente requiere involucrar al dueño del producto, no es puramente técnica.
5. Dejar el harness ejecutable de forma repetible (comando único, documentado en el README del módulo `renovatio-evals` o similar) para que corridas futuras (nuevas versiones del prompt, nuevos proyectos de prueba) sean triviales de lanzar.

## Criterios de aceptación
- [ ] El harness corre ambos modos sobre CardDemo y produce un reporte de precisión/recall.
- [ ] La relación real `XREF-FILE → ACCTFILE-FILE` (o su forma post-merge, `XREF-FILE → ACCOUNT`) es detectada en el modo LLM-asistido.
- [ ] Los 4 falsos positivos conocidos (`ACCT-FILE`/`ACCOUNT-FILE`/`ACCTFILE-FILE` entre sí, y el análogo de `CUST-FILE`/`CUSTOMER-FILE`) NO reaparecen en ninguno de los dos modos.
- [ ] Costo/latencia de la corrida LLM-asistida queda reportado y documentado.
- [ ] El harness es ejecutable con un comando documentado, sin pasos manuales ad-hoc.

## Evidencia a dejar en el PR
- Reporte de precisión/recall de la corrida sobre CardDemo.
- Reporte de costo/latencia.
- Comando/instrucciones para re-ejecutar el harness.

## Acceptance criteria

- [ ] **xref-detected:** XREF-FILE -> ACCTFILE-FILE (o ACCOUNT post-merge) es detectada en modo LLM-asistido; stages: none
- [ ] **no-false-positives:** Los 4 falsos positivos conocidos no reaparecen en ningún modo; stages: none
- [ ] **cost-reported:** Costo/latencia de la corrida LLM-asistida queda reportado; stages: none
- [ ] **repeatable:** Harness ejecutable con un comando documentado; stages: none

## Required artifacts

- none
