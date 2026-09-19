---
schema: "agora/work/v1"
id: "domain-entity-inference-coordinator"
swarm: "llm-domain-entities-epic"
title: "Coordinator: ensamblar facts-block, invocar cobol.domain.entities.v1, wiring opcional a SemanticDomainProjector"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"facts-block-buildable":"Facts-block se construye para CBACT01C/CBTRN02C/CBACT04C sin excepciones","coordinator-invokes":"Coordinator invoca el prompt v\u00eda LlmProvider (fake/offline en tests) y aplica ambos validadores","no-regression-when-disabled":"Con el coordinator deshabilitado, SemanticDomainProjector se comporta igual que antes","output-separated":"Resultado del coordinator queda separado/marcado, no mezclado silenciosamente","tests-pass":"mvn -pl renovatio-llm,renovatio-domain-model -am test -Djacoco.skip=true pasa"}
satisfied-criteria: []
criterion-statuses: {"facts-block-buildable":[],"coordinator-invokes":[],"no-regression-when-disabled":[],"output-separated":[],"tests-pass":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-domain-entities-epic/epic-llm-domain-entities"
---

# Coordinator: ensamblar facts-block, invocar cobol.domain.entities.v1, wiring opcional a SemanticDomainProjector

## Description

Parte de epic-llm-domain-entities. Depende de issues #2 y #3 (prompt y validador de grounding cerrados). Es la issue más grande del epic — considerar partirla si al empezar resulta demasiado grande para un solo PR.

## Objetivo
Implementar el coordinator que arma el "facts block" determinista desde el IR ya extraído (`CobolIntermediateModel`/`SemanticProgram`), invoca el prompt `cobol.domain.entities.v1` vía el `LlmProvider` existente, valida el resultado, y lo entrega como input opcional adicional a `SemanticDomainProjector` — sin reemplazar ni borrar el código determinista actual, que debe seguir funcionando como fallback real (no solo declarado en YAML).

## Contexto técnico necesario
- Precedente arquitectónico directo a replicar: `renovatio-llm/src/main/java/org/modernash/renovatio/llm/residual/ResidualEnrichmentCoordinator.java` (+ `ResidualEnrichmentExecutor.java`, `ResidualRouter.java`, `ResidualEnrichmentRequest.java`, `ResidualEnrichmentOutcome.java`) — es el coordinator existente que arma el request para los prompts `DATA_INTENT.*`, invoca el LLM, valida, y produce `AnnotatedCobolContext` que `CobolSemanticProjector` consume. El nuevo coordinator para domain-entities debe seguir el mismo patrón de responsabilidades (armar request → invocar → validar → producir un resultado tipado), no necesariamente heredar de las mismas clases.
- **Fuentes de hechos deterministas ya implementadas y testeadas** (todas de esta sesión de trabajo, ver git diff/historial si no está commiteado aún):
  - `CobolIntermediateModel.getFileKeyFields()` → `Map<String, List<String>>`: SELECT name → campos de RECORD KEY/ALTERNATE RECORD KEY.
  - `CobolIntermediateModel.getFileAssignTarget()` → `Map<String, String>`: SELECT name → dataset físico de `ASSIGN TO`.
  - `CobolIntermediateModel.getFileToRecordMapping()` (preexistente): FD name → nombre del record 01-level.
  - `SemanticProgram.types()` con `memberIds` pobladas (grupos COBOL con sus campos hijos).
  - `SemanticProgram.FieldFlow` (`sourceTypeId`, `targetTypeId`): pares de cada `MOVE` de un solo campo a otro, con ambos extremos resueltos a un dato real — ver `CobolSemanticProjector.recordFieldFlow(...)`.
  - `SemanticDomainProjector` ya construye internamente varios de estos mapas por programa (`keyFieldsByRepositoryName`, `assignTargetByRepositoryName`, `fieldTypeIdToRepositoryName`, `typeIdToFieldName`, `boundaryStructuralRecordById`) — revisar si conviene exponerlos/refactorizarlos para que el coordinator los reutilice en vez de recalcularlos, o si el coordinator debe construir su propia vista de "hechos" directamente desde `CobolIntermediateModel`/`SemanticProgram` (más desacoplado, pero duplica algo de lógica de recorrido).
- Punto de integración con `SemanticDomainProjector`: hoy `project(String projectId, List<SemanticProgram> programs)` es el método principal, y llama internamente a `mergeCrossProgramRepositories(...)` y luego `inferForeignKeyRelations(...)`/`inferForeignKeyRelationsFromDataFlow(...)` antes de devolver el `DomainModel`. El resultado del coordinator LLM debería entrar como relaciones/entidades ADICIONALES (marcadas con un origen distinto — ver `DomainModel.Origin`, hoy solo `Origin.DETERMINISTIC` está en uso; puede necesitar un nuevo valor de enum, ej. `Origin.LLM_SUGGESTED`, sujeto a que pase por el gate de revisión humana de la issue #5 antes de quedar como `DETERMINISTIC`/aprobado) — NO debe reemplazar ni pisar las relaciones deterministas ya inferidas.
- Revisar `LlmProvider`/`AnthropicLlmProvider`/`CircuitBreakerLLMProvider`/`OfflineFakeProvider` (`renovatio-llm-runtime`) para el mecanismo real de invocación, y `BudgetEnforcer`/`ContentAddressedCache` para entender cómo aplicar límite de costo y cacheo (importante: un proyecto como CardDemo tiene 44 programas — invocar el prompt por cada programa individualmente puede ser costoso; evaluar si conviene batchear varios programas relacionados en una sola invocación, o invocar solo sobre los "boundary"/REPOSITORY candidatos ya identificados deterministamente en vez de sobre cada programa completo).
- El **gate de revisión humana** es una issue separada (#5) pero el coordinator debe dejar el resultado en un estado "propuesto, no aplicado" listo para ese gate, no persistirlo directamente como verdad en el `DomainModel`.

## Tareas
1. Diseñar y documentar la forma exacta del "facts block" (probablemente un DTO/record Java, ej. `DomainInferenceFacts`) que se serializa al prompt — debe incluir, por cada FD/SELECT: nombre, `assignTarget`, `recordKey`(s), campos (incluyendo los de copybooks ya expandidos), y la lista de `FieldFlow` relevantes (source/target por nombre de campo, no por typeId interno).
2. Implementar el ensamblado de ese DTO desde `CobolIntermediateModel`/`SemanticProgram` para uno o más programas de un proyecto.
3. Implementar el coordinator (`DomainEntityInferenceCoordinator` o nombre similar) que arme el request del prompt, invoque `LlmProvider` con el `promptId: cobol.domain.entities.v1`, aplique los validadores (`json-schema.v1`, `domain-entities-reference.v1` de la issue #3), y produzca un resultado tipado (entidades/relaciones propuestas, con su `confidence`/`rationale` del schema) o el fallback determinista si falla.
4. Decidir y documentar la política de invocación (por programa vs. por proyecto vs. por conjunto de boundaries) considerando costo/latencia — dejar constancia de la decisión y su justificación en el PR.
5. Wiring: exponer el resultado del coordinator como una fuente ADICIONAL y claramente separada de relaciones/entidades propuestas, sin modificar el comportamiento actual de `SemanticDomainProjector.project(...)` cuando el coordinator no está habilitado/disponible (debe poder desactivarse limpiamente, ej. vía flag/config, degradando a exactamente el comportamiento actual).
6. Tests: unitarios del ensamblado del facts-block (dado un `CobolIntermediateModel`/`SemanticProgram` sintético, verificar el DTO producido), y de integración del coordinator usando `OfflineFakeProvider`/`FakeLLMProvider` (ya existentes en `renovatio-llm-runtime`/`renovatio-llm`) para no depender de una API real en tests.

## Criterios de aceptación
- [ ] El facts-block se puede construir para al menos los programas `CBACT01C`, `CBTRN02C`, `CBACT04C` de CardDemo (los que se investigaron manualmente esta sesión) sin excepciones.
- [ ] El coordinator invoca el prompt vía `LlmProvider` (probado con un fake/offline provider, sin requerir credenciales reales en CI) y aplica ambos validadores.
- [ ] Con el coordinator deshabilitado, `SemanticDomainProjector.project(...)` se comporta exactamente igual que antes de esta issue (sin regresiones — correr toda la suite de `renovatio-domain-model`).
- [ ] El resultado del coordinator queda expuesto de forma separada/marcada (no mezclado silenciosamente con las relaciones deterministas ya existentes).
- [ ] `mvn -pl renovatio-llm,renovatio-domain-model -am test -Djacoco.skip=true` pasa.

## Evidencia a dejar en el PR
- Output de los tests de ensamblado del facts-block y del coordinator.
- Documentación de la decisión de política de invocación (por programa/proyecto/boundary) y su costo estimado para un proyecto del tamaño de CardDemo (44 programas).

## Acceptance criteria

- [ ] **facts-block-buildable:** Facts-block se construye para CBACT01C/CBTRN02C/CBACT04C sin excepciones; stages: none
- [ ] **coordinator-invokes:** Coordinator invoca el prompt vía LlmProvider (fake/offline en tests) y aplica ambos validadores; stages: none
- [ ] **no-regression-when-disabled:** Con el coordinator deshabilitado, SemanticDomainProjector se comporta igual que antes; stages: none
- [ ] **output-separated:** Resultado del coordinator queda separado/marcado, no mezclado silenciosamente; stages: none
- [ ] **tests-pass:** mvn -pl renovatio-llm,renovatio-domain-model -am test -Djacoco.skip=true pasa; stages: none

## Required artifacts

- none
