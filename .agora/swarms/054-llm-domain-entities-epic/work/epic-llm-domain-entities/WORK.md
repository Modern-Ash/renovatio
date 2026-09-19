---
schema: "agora/work/v1"
id: "epic-llm-domain-entities"
swarm: "llm-domain-entities-epic"
title: "Epic: Inferencia de entidades/relaciones de dominio COBOL v\u00eda LLM gobernado"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"facts-block-complete":"El facts-block puede construirse completo para CardDemo incluyendo campos de copybooks (bug de shadowing corregido)","prompt-e2e":"cobol.domain.entities.v1 puede invocarse end-to-end contra un programa real de CardDemo y producir relaciones v\u00e1lidas contra schema","grounding-validator":"Un validador de grounding rechaza determin\u00edsticamente cualquier output que referencie un campo/entidad no presente en los hechos suministrados","human-gate":"Existe un gate de revisi\u00f3n humana antes de persistir una relaci\u00f3n sugerida por LLM en el DomainModel","eval-harness":"El eval harness reporta precisi\u00f3n/recall LLM-asistido vs determinista sobre el ground-truth de CardDemo (XREF-FILE -> ACCTFILE-FILE detectado, los 4 falsos positivos conocidos no reaparecen)","clean-fallback":"Si el LLM falla o no est\u00e1 disponible, el sistema degrada limpiamente al comportamiento determinista actual"}
satisfied-criteria: []
criterion-statuses: {"facts-block-complete":[],"prompt-e2e":[],"grounding-validator":[],"human-gate":[],"eval-harness":[],"clean-fallback":[]}
required-artifacts: []
child-work-refs: ["llm-domain-entities-epic/fix-copybook-index-shadowing","llm-domain-entities-epic/close-domain-entities-prompt-prototype","llm-domain-entities-epic/domain-entities-grounding-validator","llm-domain-entities-epic/domain-entity-inference-coordinator","llm-domain-entities-epic/domain-entities-human-review-gate","llm-domain-entities-epic/domain-entities-eval-harness"]
budget-limits: null
---

# Epic: Inferencia de entidades/relaciones de dominio COBOL vía LLM gobernado

## Description

## Objetivo

Reemplazar el núcleo heurístico de `SemanticDomainProjector` (dedupe de entidades FILE/RECORD, fusión cross-programa de repositorios, inferencia de foreign keys) por un prompt LLM gobernado (`cobol.domain.entities.v1`) que reciba únicamente hechos deterministas ya extraídos por el parser COBOL y proponga entidades/relaciones como JSON validado por schema — manteniendo el código actual como fallback determinista y añadiendo un validador de "grounding" que rechace cualquier campo/entidad inventado por el modelo.

Este epic nace de una sesión de trabajo (2026-09-15/16) en la que se pasaron horas endureciendo a mano `SemanticDomainProjector.java` (fusión de FILE+RECORD, unión por `ASSIGN TO` físico, inferencia de FK por `RECORD KEY`, inferencia por flujo de datos vía `MOVE`) y `SimpleCobolIrParser.java` (expansión de COPY, extracción de `RECORD KEY`/`ASSIGN TO`) contra el proyecto real AWS CardDemo, con múltiples rondas de falsos positivos/negativos por heurísticas de nombre. Un agente de investigación confirmó que Renovatio ya tiene una infraestructura LLM gobernada madura (`renovatio-llm`, `renovatio-llm-runtime`, 10 tipos de prompt existentes) que hoy solo se usa para anotaciones residuales puntuales (REDEFINES/OCCURS DEPENDING ON/MOVE CORRESPONDING), y que agregar un tipo de prompt nuevo es "YAML + JSON schema + fallback", no infraestructura nueva.

## Acceptance criteria

- [ ] **facts-block-complete:** El facts-block puede construirse completo para CardDemo incluyendo campos de copybooks (bug de shadowing corregido); stages: none
- [ ] **prompt-e2e:** cobol.domain.entities.v1 puede invocarse end-to-end contra un programa real de CardDemo y producir relaciones válidas contra schema; stages: none
- [ ] **grounding-validator:** Un validador de grounding rechaza determinísticamente cualquier output que referencie un campo/entidad no presente en los hechos suministrados; stages: none
- [ ] **human-gate:** Existe un gate de revisión humana antes de persistir una relación sugerida por LLM en el DomainModel; stages: none
- [ ] **eval-harness:** El eval harness reporta precisión/recall LLM-asistido vs determinista sobre el ground-truth de CardDemo (XREF-FILE -> ACCTFILE-FILE detectado, los 4 falsos positivos conocidos no reaparecen); stages: none
- [ ] **clean-fallback:** Si el LLM falla o no está disponible, el sistema degrada limpiamente al comportamiento determinista actual; stages: none

## Required artifacts

- none
