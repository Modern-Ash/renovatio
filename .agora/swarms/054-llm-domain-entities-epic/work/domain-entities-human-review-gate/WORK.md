---
schema: "agora/work/v1"
id: "domain-entities-human-review-gate"
swarm: "llm-domain-entities-epic"
title: "Gate de revisi\u00f3n humana para entidades/relaciones propuestas por LLM"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"partial-review":"Se puede aceptar/rechazar entidades y relaciones individualmente dentro de un batch","only-accepted-merged":"Solo lo aceptado se fusiona al DomainModel, lo rechazado no deja rastro","reuses-merge-logic":"La fusi\u00f3n reutiliza la l\u00f3gica de dedupe/merge existente en SemanticDomainProjector","auto-review-mode":"Existe modo auto-review documentado como exclusivo para evaluaci\u00f3n"}
satisfied-criteria: []
criterion-statuses: {"partial-review":[],"only-accepted-merged":[],"reuses-merge-logic":[],"auto-review-mode":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-domain-entities-epic/epic-llm-domain-entities"
---

# Gate de revisión humana para entidades/relaciones propuestas por LLM

## Description

Parte de epic-llm-domain-entities. Depende de issue #4 (coordinator produciendo resultados propuestos). Puede diseñarse en paralelo con #4/#6 si se acuerda el contrato de datos de antemano.

## Objetivo
Extender el patrón de revisión humana que ya existe para anotaciones LLM de un solo campo (`HumanAnnotationReviewService`/`AnnotationReview.ReviewState`) para poder aceptar/rechazar, entidad por entidad y relación por relación, un grafo completo de entidades/relaciones propuesto por `cobol.domain.entities.v1`, antes de que cualquiera de ellas se persista como parte "confirmada" del `DomainModel`.

## Contexto técnico necesario
- Precedente a extender: `HumanAnnotationReviewService.java` (buscar en `renovatio-provider-cobol` o `renovatio-llm`, revisar su test `HumanAnnotationReviewServiceTest.java`) y `AnnotationReview`/`ReviewState` (`ACCEPTED`/etc., visto usado en `CobolSemanticProjector.contributingAnnotations` filtrando por `review().reviewState() == AnnotationReview.ReviewState.ACCEPTED`). Hoy este mecanismo gatea anotaciones de intención sobre UN campo (REDEFINES/OCCURS/MOVE CORRESPONDING) — acá hace falta gatear un conjunto de N entidades y M relaciones juntas, potencialmente con revisión parcial (aceptar algunas relaciones y rechazar otras del mismo batch).
- Revisar cómo el Workbench/VS Code extension ya expone flujos de revisión/aprobación existentes para no inventar una UX nueva de cero — ver `WorkbenchChangeSet`/`changeSetAction` (`submit-review`/`approve`/`apply`/`rollback`) mencionado en el Epic #263 (`renovatio-api`) como el mecanismo general de "aplicar cambios gobernados" ya existente en el proyecto; evaluar si conviene modelar la propuesta de entidades/relaciones LLM como un tipo de `WorkbenchChangeSet` en vez de un mecanismo paralelo nuevo.
- Definir dónde vive el estado de "propuesta pendiente de revisión": ¿en el propio `DomainModel` con un campo de origen/estado por nodo/relación (requeriría extender `DomainNode`/`DomainRelation` con algo como `Origin.LLM_SUGGESTED` + un estado de revisión), o en una tabla/estructura separada que solo se fusiona al `DomainModel` una vez aprobada? La segunda opción es más segura (no ensucia el modelo "de verdad" con propuestas no revisadas) pero requiere un paso de fusión explícito — se recomienda esta segunda opción, consistente con cómo `mergeCrossProgramRepositories`/`inferForeignKeyRelations` ya construyen el modelo en pasos claramente separados dentro de `SemanticDomainProjector.project(...)`.
- Revisar qué necesita el eval harness (issue #6) de este mecanismo — probablemente necesita poder correr en modo "sin gate humano" (auto-aceptar todo lo que pase el validador de grounding) para poder medir precisión/recall de forma no interactiva; diseñar el servicio de forma que soporte ambos modos (interactivo con revisión humana real, y modo "auto-review" para evaluación automatizada) sin duplicar código.

## Tareas
1. Diseñar el contrato de datos de una "propuesta de revisión" que agrupe N entidades + M relaciones de una sola invocación del coordinator (issue #4), con estado individual por elemento (pendiente/aceptado/rechazado) y estado global del batch.
2. Implementar el servicio de revisión (extendiendo o creando junto a `HumanAnnotationReviewService`) con esa granularidad.
3. Definir cómo una propuesta aceptada se fusiona al `DomainModel` real — probablemente un paso adicional en `SemanticDomainProjector` (o un servicio separado que lo invoca después) que toma las entidades/relaciones aceptadas y las agrega con el mismo rigor de dedupe/merge que ya existe para las deterministas (para no reintroducir los mismos bugs de duplicados que costó tanto corregir esta sesión).
4. Implementar el "modo auto-review" para uso del eval harness (issue #6), documentado explícitamente como solo para evaluación, no para uso en producción sin supervisión.
5. Tests: batch con revisión parcial (algunas entidades aceptadas, otras rechazadas), batch completo aceptado, batch completo rechazado, y verificación de que solo lo aceptado llega a fusionarse al `DomainModel`.

## Criterios de aceptación
- [ ] Se puede aceptar/rechazar entidades y relaciones individualmente dentro de un mismo batch propuesto.
- [ ] Solo las entidades/relaciones aceptadas se fusionan al `DomainModel`; las rechazadas no dejan rastro en el modelo persistido.
- [ ] La fusión de lo aceptado reutiliza (no duplica) la lógica de dedupe/merge ya existente en `SemanticDomainProjector` para evitar reintroducir duplicados.
- [ ] Existe un modo "auto-review" claramente documentado como exclusivo para evaluación automatizada (issue #6), no para producción.

## Evidencia a dejar en el PR
- Tests cubriendo revisión parcial, total-aceptado, total-rechazado.
- Documentación del contrato de datos de la propuesta de revisión.

## Acceptance criteria

- [ ] **partial-review:** Se puede aceptar/rechazar entidades y relaciones individualmente dentro de un batch; stages: none
- [ ] **only-accepted-merged:** Solo lo aceptado se fusiona al DomainModel, lo rechazado no deja rastro; stages: none
- [ ] **reuses-merge-logic:** La fusión reutiliza la lógica de dedupe/merge existente en SemanticDomainProjector; stages: none
- [ ] **auto-review-mode:** Existe modo auto-review documentado como exclusivo para evaluación; stages: none

## Required artifacts

- none
