---
schema: "agora/work/v1"
id: "close-domain-entities-prompt-prototype"
swarm: "llm-domain-entities-epic"
title: "Cerrar prototipo del prompt cobol.domain.entities.v1: tests + contrato documentado"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"prompt-tests":"Test dedicado del prompt cubre los campos clave del PromptDefinition","schema-tests":"Tests del JSON schema cubren 1 caso positivo y 3 negativos","cardinality-decision":"Decisi\u00f3n sobre cardinality opcional/requerido documentada","llm-tests-pass":"mvn -pl renovatio-llm -am test -Djacoco.skip=true pasa"}
satisfied-criteria: []
criterion-statuses: {"prompt-tests":[],"schema-tests":[],"cardinality-decision":[],"llm-tests-pass":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-domain-entities-epic/epic-llm-domain-entities"
---

# Cerrar prototipo del prompt cobol.domain.entities.v1: tests + contrato documentado

## Description

Parte de epic-llm-domain-entities.

## Objetivo
Cerrar y endurecer el prototipo del prompt `cobol.domain.entities.v1` ya iniciado en este working tree (sin commitear): agregar tests unitarios propios del contrato (hoy solo lo cubre indirectamente `PromptCatalogLoaderTest`) y documentar el contrato de entrada/salida.

## Contexto técnico necesario
- Archivos ya creados en esta sesión (revisar su estado actual en el working tree antes de asumir que están commiteados — puede que otra issue los haya tocado o commiteado ya):
  - `renovatio-llm/src/main/resources/schemas/domain-entities.v1.schema.json` — JSON schema del output.
  - `renovatio-llm/src/main/resources/prompts/cobol.domain.entities.v1.yaml` — definición del prompt (`promptId`, `appliesTo: DOMAIN_MODEL.ENTITY_RELATIONS`, `system`, `fewShot`, `outputSchema`, `validators: [json-schema.v1]`, `fallback`).
  - `renovatio-llm/src/main/resources/fallbacks/cobol.domain.entities.fallback.v1.yaml` — fallback `MANUAL_ACTION`.
  - `renovatio-llm/src/main/resources/prompts/catalog-v1.yaml` — se agregó la entrada al índice.
  - `renovatio-llm/src/main/java/org/modernash/renovatio/llm/prompt/PromptCatalogLoader.java` — se agregó `"DOMAIN_MODEL.ENTITY_RELATIONS"` al `SELECTORS` (línea ~25-29).
  - `renovatio-llm/src/test/java/org/modernash/renovatio/llm/prompt/PromptCatalogLoaderTest.java` — se actualizó la lista esperada de `promptId`s.
- Verificado: `mvn -pl renovatio-llm -am clean test -Djacoco.skip=true` pasa (87 tests) con estos cambios — el catálogo carga y valida el nuevo prompt correctamente contra el `PromptCatalogLoader` existente (`VERSIONED_ID`, `VERSIONED_SCHEMA`, `SELECTORS`, `VALIDATORS` checks, más la validación estructural del fallback en `validateFallback`).
- Revisar el patrón de tests existente para otros prompts (ej. `renovatio-llm/src/test/java/.../` — buscar tests que carguen un `PromptDefinition` específico y verifiquen sus campos) para replicar el mismo nivel de cobertura para `cobol.domain.entities.v1`.
- El schema actual (`domain-entities.v1.schema.json`) es deliberadamente plano/simple — evaluar si alcanza o si hace falta iterar: soporta `entities[].{name,sourceFacts[],fields[]}` y `relations[].{fromEntity,toEntity,evidenceField,cardinality?,confidence,rationale}`. **Nota abierta**: no valida que `cardinality` sea obligatorio (es opcional en el schema actual) — decidir si eso es correcto o si debería ser requerido.

## Tareas
1. Escribir un test dedicado (ej. `CobolDomainEntitiesPromptTest.java` en `renovatio-llm/src/test/java/org/modernash/renovatio/llm/prompt/`) que cargue específicamente `cobol.domain.entities.v1` del catálogo y verifique: `appliesTo() == "DOMAIN_MODEL.ENTITY_RELATIONS"`, `outputSchema()` apunta al schema correcto, `validators()` contiene `json-schema.v1`, y el `fewShot` de ejemplo (el caso XREF-FILE → ACCOUNT de CardDemo) es válido contra el JSON schema.
2. Escribir tests directos del JSON schema (`domain-entities.v1.schema.json`) con casos positivos (el fewShot) y negativos (ej. `relations[]` sin `evidenceField`, `entities[]` sin `sourceFacts`, campos extra no declarados por `additionalProperties: false`) — revisar cómo otros schemas del proyecto (`decision-suggestion.v1.schema.json`, `data-intent.v1.schema.json`) son testeados para seguir el mismo patrón/librería de validación JSON schema ya usada en el repo.
3. Revisar y decidir sobre la nota abierta de `cardinality` opcional vs. requerido; ajustar el schema si corresponde y documentar la decisión en un comentario o en el propio issue/PR.
4. Documentar el contrato del prompt (input esperado del "facts block", forma del output) en un README o comentario en el propio YAML — pensando en que la issue #4 (coordinator) necesita saber exactamente qué forma de facts-block construir.

## Criterios de aceptación
- [ ] Test dedicado del prompt `cobol.domain.entities.v1` pasa y cubre los campos clave del `PromptDefinition`.
- [ ] Tests del JSON schema cubren al menos un caso positivo y tres negativos (relation sin evidenceField, entity sin sourceFacts, output con campo extra no declarado).
- [ ] Decisión sobre `cardinality` documentada y reflejada en el schema si aplica.
- [ ] `mvn -pl renovatio-llm -am test -Djacoco.skip=true` sigue pasando.

## Evidencia a dejar en el PR
- Output de los tests nuevos.
- Output de `mvn -pl renovatio-llm -am test -Djacoco.skip=true`.

## Acceptance criteria

- [ ] **prompt-tests:** Test dedicado del prompt cubre los campos clave del PromptDefinition; stages: none
- [ ] **schema-tests:** Tests del JSON schema cubren 1 caso positivo y 3 negativos; stages: none
- [ ] **cardinality-decision:** Decisión sobre cardinality opcional/requerido documentada; stages: none
- [ ] **llm-tests-pass:** mvn -pl renovatio-llm -am test -Djacoco.skip=true pasa; stages: none

## Required artifacts

- none
