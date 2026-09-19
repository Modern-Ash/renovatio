---
schema: "agora/work/v1"
id: "residual-discovery-llm-routing"
swarm: "llm-cobol-discovery-epic"
title: "Rutear UnclassifiedDataAccess/residual a clasificaci\u00f3n LLM de discovery"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"prompt-decision":"Decisi\u00f3n prompt reusado vs nuevo documentada y justificada","residual-classified":"Un UnclassifiedDataAccess real recibe clasificaci\u00f3n candidata de baja confianza","additive-only":"El mecanismo es puramente aditivo, deshabilitado no cambia comportamiento","ui-exposure-investigated":"Se investig\u00f3 c\u00f3mo exponer esto en el Workbench reusando el mecanismo de la issue #272"}
satisfied-criteria: []
criterion-statuses: {"prompt-decision":[],"residual-classified":[],"additive-only":[],"ui-exposure-investigated":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-cobol-discovery-epic/epic-llm-cobol-discovery"
---

# Rutear UnclassifiedDataAccess/residual a clasificación LLM de discovery

## Description

Parte de epic-llm-cobol-discovery.

## Objetivo
Extender el uso del prompt gobernado `cobol.unsupported.explain.v1` (hoy usado solo en la fase de generación de código, para explicar por qué una construcción no se puede traducir) a la fase de *discovery*: cuando el parser encuentra un acceso a dato que no puede clasificar (`SemanticProgram.UnclassifiedDataAccess`, poblado como `residual` en `CobolSemanticProjector`), rutearlo al mismo prompt (o a uno hermano específico de discovery) para obtener una clasificación candidata de baja confianza en vez de dejarlo huérfano y silencioso.

## Contexto técnico necesario
- Prompt existente: `renovatio-llm/src/main/resources/prompts/cobol.unsupported.explain.v1.yaml`:
  ```yaml
  promptId: cobol.unsupported.explain.v1
  appliesTo: UNSUPPORTED_EXPLANATION
  system: Explain why a COBOL construction cannot be translated and draft a manual action.
  fewShot:
    - input: {construction: ALTER}
      output: {construction: ALTER, explanation: Runtime target mutation is unsupported, manualAction: Replace with explicit reviewed dispatch}
  outputSchema: schemas/unsupported-explanation.v1.schema.json
  validators: [json-schema.v1, annotated-ir-reference.v1, sanitized-persistence.v1]
  fallback: fallbacks/cobol.unsupported.explain.fallback.v1.yaml
  ```
  Su propósito actual (`system` prompt) es explicar por qué NO SE PUEDE traducir algo — orientado a codegen, no a ayudar a clasificar qué ES algo durante el discovery. Puede no ser el prompt correcto para reusar tal cual; evaluar si conviene un prompt hermano nuevo (`cobol.discovery.classify-residual.v1` o similar) con un `system` distinto ("clasifica qué tipo de acceso a dato es esto, dado el contexto") en vez de forzar el reuso de uno pensado para otra cosa.
  Revisar también `schemas/unsupported-explanation.v1.schema.json` para ver si su forma (`construction`/`explanation`/`manualAction`) es compatible con lo que hace falta acá o si un discovery-classifier necesita un schema distinto (probablemente sí: acá lo que se necesita es una clasificación tipo `IoKind`/`EffectKind` candidata, no una explicación de por qué algo falla).
- Puntos de generación de `UnclassifiedDataAccess`/`residual` en `CobolSemanticProjector.java`: la lista `residual` (`List<SemanticProgram.UnclassifiedDataAccess>`) se pasa por referencia a través de `visitStatements`/`recordExpressionReads`/`recordStateAccess`/`recordRead`/`recordWrite` (buscar `residual` en el archivo — aparece en las firmas de `recordStateAccess` alrededor de la línea 312-329 del archivo tal como estaba antes de los cambios de esta sesión; confirmar línea exacta al momento de implementar, el archivo se modificó bastante recientemente). La condición que decide "esto es residual" es cuando `recordStateAccess` no encuentra un `typeId` para el `subject` en el mapa `typeIds` (es decir, el parser no pudo asociar el nombre referenciado con ningún dato conocido) — revisar `SemanticProgram.UnclassifiedDataAccess` (record con `subject`, `observedOperation`, `reason`, `evidenceIds`) para entender qué información ya está disponible para pasarle al prompt sin tener que re-extraer nada.
- Este es el candidato de MENOR riesgo del epic para implementar rápido: no cambia ningún comportamiento existente si se apaga, solo AGREGA una clasificación candidata (de baja confianza, gateada) a algo que hoy simplemente se descarta/ignora — no hay heurística hardcodeada que reemplazar ni riesgo de romper un flujo existente.

## Tareas
1. Decidir si reusar `cobol.unsupported.explain.v1` (ajustando su `system`/schema si hace falta, con cuidado de no romper su uso actual en codegen) o crear un prompt hermano nuevo específico de discovery — se recomienda un prompt nuevo si el schema/propósito no calzan limpio, siguiendo el mismo patrón YAML/schema/fallback que el resto del epic.
2. Diseñar el schema de salida: clasificación candidata (ej. mapear a `IoKind`/`EffectKind` del propio `SemanticProgram`, o una categoría más simple si no aplica un mapeo 1:1), confianza, rationale.
3. Implementar el wiring: cuando `recordStateAccess` (o el punto equivalente tras refactors posteriores) determina que algo es residual, en vez de (o además de) agregarlo solo a la lista `residual`, invocar el prompt de clasificación y adjuntar el resultado como metadata de baja confianza — sin que esto bloquee ni cambie el pipeline determinista existente (es un enriquecimiento aditivo).
4. Definir cómo se expone esta clasificación candidata aguas abajo (¿aparece en el Workbench como sugerencia sobre el nodo, similar a lo que ya hace el epic #263/#272 "AI-assisted: sugerencias LLM gobernadas directamente sobre el nodo del canvas"? — revisar issue #272 cerrada para ver si ya hay un mecanismo de UI reusable para "sugerencia LLM sobre un nodo").
5. Tests: al menos un caso de `UnclassifiedDataAccess` real (buscar en fixtures existentes de `renovatio-provider-cobol`/`renovatio-cobol-ir` algún programa de prueba que genere residuales, o construir uno sintético) que reciba una clasificación candidata del prompt.

## Criterios de aceptación
- [ ] Decisión documentada: prompt reusado vs. prompt nuevo, con justificación.
- [ ] Un `UnclassifiedDataAccess` real recibe una clasificación candidata de baja confianza sin alterar el comportamiento determinista existente.
- [ ] El mecanismo es puramente aditivo — deshabilitado, el sistema se comporta exactamente igual que hoy.
- [ ] Se investigó (no necesariamente implementó) cómo exponer esto en el Workbench reusando el mecanismo de sugerencias LLM de la issue #272, si existe.

## Evidencia a dejar en el PR
- Test con un `UnclassifiedDataAccess` real clasificado.
- Confirmación de que el pipeline determinista no cambia si el enriquecimiento está deshabilitado.

## Acceptance criteria

- [ ] **prompt-decision:** Decisión prompt reusado vs nuevo documentada y justificada; stages: none
- [ ] **residual-classified:** Un UnclassifiedDataAccess real recibe clasificación candidata de baja confianza; stages: none
- [ ] **additive-only:** El mecanismo es puramente aditivo, deshabilitado no cambia comportamiento; stages: none
- [ ] **ui-exposure-investigated:** Se investigó cómo exponer esto en el Workbench reusando el mecanismo de la issue #272; stages: none

## Required artifacts

- none
