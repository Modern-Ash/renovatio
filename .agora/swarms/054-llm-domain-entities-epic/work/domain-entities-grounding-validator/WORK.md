---
schema: "agora/work/v1"
id: "domain-entities-grounding-validator"
swarm: "llm-domain-entities-epic"
title: "Validador de grounding domain-entities-reference.v1 (anti-alucinaci\u00f3n)"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"rejects-hallucination":"Rechaza output que referencia campo no presente en los hechos suministrados","accepts-grounded":"Acepta el output del fewShot del prompt (grounded)","registered":"Validador registrado y declarable desde cobol.domain.entities.v1.yaml"}
satisfied-criteria: []
criterion-statuses: {"rejects-hallucination":[],"accepts-grounded":[],"registered":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-domain-entities-epic/epic-llm-domain-entities"
---

# Validador de grounding domain-entities-reference.v1 (anti-alucinación)

## Description

Parte de epic-llm-domain-entities. Depende de issue #2 (prompt cerrado) — puede empezar en paralelo si el schema ya está estable.

## Objetivo
Implementar un validador de "grounding" (`domain-entities-reference.v1`) que rechace cualquier output del prompt `cobol.domain.entities.v1` cuyo `entities[].sourceFacts[]` o `relations[].evidenceField` referencie un hecho (nombre de campo, FD, archivo) que no estaba literalmente presente en los hechos suministrados en el prompt — la defensa anti-alucinación central de este epic.

## Contexto técnico necesario
- El prompt (`renovatio-llm/src/main/resources/prompts/cobol.domain.entities.v1.yaml`) instruye explícitamente al modelo a no inventar campos, pero eso es una instrucción de prompt, no una garantía — se necesita un validador de código que lo verifique mecánicamente, igual que ya existe para otro caso de uso.
- Precedente directo a estudiar: `renovatio-llm/src/main/java/org/modernash/renovatio/llm/prompt/AnnotatedIrSemanticOutputValidator.java` — implementa el validador `annotated-ir-reference.v1` usado por los prompts `DATA_INTENT.*`, que verifica que el output del LLM referencia nodos reales del IR anotado. Es el patrón más cercano a lo que hace falta acá, pero opera sobre `AnnotationFamily`/nodeId del IR, no sobre el facts-block de este prompt nuevo — no se puede reusar tal cual, pero sí el patrón de implementación (dónde vive, cómo se registra, cómo se invoca).
- Revisar `renovatio-llm/src/main/java/org/modernash/renovatio/llm/prompt/PromptOutputValidator.java` (interfaz que implementan los validadores) y `PromptCatalogLoader.java` (`VALIDATORS` set, línea ~30-32) para entender cómo un nuevo id de validador (`domain-entities-reference.v1`) se registra y se hace disponible para que `cobol.domain.entities.v1.yaml` lo declare en su lista `validators:`.
- El validador necesita acceso al facts-block ORIGINAL que se envió en el prompt (no solo al output) para poder comparar — revisar cómo el flujo de invocación real (`ResidualEnrichmentExecutor`/`ResidualEnrichmentCoordinator` o el nuevo coordinator de la issue #4) pasa el input junto con el output al momento de validar, para diseñar la firma del nuevo validador de forma consistente.
- El schema del facts-block todavía no está formalizado como tipo Java — probablemente se defina en la issue #4 (coordinator) como parte del ensamblado; coordinar con esa issue para no duplicar la definición del "hecho" (nombre de campo/FD/ASSIGN TO/RECORD KEY) en dos lugares.

## Tareas
1. Definir la interfaz de "hechos" que el validador puede verificar contra — probablemente un `Set<String>` de todos los nombres de campo/FD/dataset mencionados literalmente en el facts-block enviado al prompt (case-insensitive, coherente con cómo el resto del código normaliza nombres COBOL a mayúsculas, ej. `SemanticDomainProjector` usa `java.util.Locale.ROOT` uppercasing consistentemente).
2. Implementar `DomainEntitiesGroundingValidator` (o el nombre que se decida) implementando `PromptOutputValidator`, que para cada `entities[].sourceFacts[]` y cada `relations[].evidenceField`/`fromEntity`/`toEntity` del output JSON verifique pertenencia al set de hechos conocidos, fallando con un código de error claro (seguir la convención de códigos existente, ej. `PROMPT_*` en `PromptCatalogException`) si algo no está grounded.
3. Registrar el nuevo validador id `domain-entities-reference.v1` en el `VALIDATORS` set de `PromptCatalogLoader.java` y actualizar `cobol.domain.entities.v1.yaml` para declararlo en `validators: [json-schema.v1, domain-entities-reference.v1]`.
4. Tests: casos donde el output está 100% grounded (pasa), y casos donde inventa un campo que no estaba en los hechos (falla con el código de error esperado) — usar el ejemplo real de CardDemo (XREF-FILE/ACCOUNT) como base y mutar el output para introducir una alucinación.

## Criterios de aceptación
- [ ] `domain-entities-reference.v1` rechaza un output que referencia un campo no presente en los hechos suministrados, con un mensaje/código de error identificable.
- [ ] `domain-entities-reference.v1` acepta el output del `fewShot` del prompt (que sí está grounded).
- [ ] El validador queda registrado y utilizable desde `cobol.domain.entities.v1.yaml`.
- [ ] Tests cubren al menos: campo alucinado en `sourceFacts`, campo alucinado en `evidenceField`, y el caso feliz completamente grounded.

## Evidencia a dejar en el PR
- Output de los tests nuevos, incluyendo el caso de rechazo por alucinación.

## Acceptance criteria

- [ ] **rejects-hallucination:** Rechaza output que referencia campo no presente en los hechos suministrados; stages: none
- [ ] **accepts-grounded:** Acepta el output del fewShot del prompt (grounded); stages: none
- [ ] **registered:** Validador registrado y declarable desde cobol.domain.entities.v1.yaml; stages: none

## Required artifacts

- none
