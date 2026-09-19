---
schema: "agora/work/v1"
id: "cics-verb-classification-llm"
swarm: "llm-cobol-discovery-epic"
title: "Reemplazar NON_PERSISTENCE_VERB_NAMES por clasificaci\u00f3n LLM gobernada de verbos CICS/SQL"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"discovery-source-found":"Punto backend donde se arma discovery.repositories identificado","no-regression-known-verbs":"Verbos ya en la lista actual clasifican igual (sin regresi\u00f3n)","generalizes-new-verb":"Al menos un verbo CICS no incluido en la lista original clasifica correctamente","clean-fallback":"Si el LLM no est\u00e1 disponible, degrada al comportamiento actual de isFileControlVerbStub"}
satisfied-criteria: []
criterion-statuses: {"discovery-source-found":[],"no-regression-known-verbs":[],"generalizes-new-verb":[],"clean-fallback":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-cobol-discovery-epic/epic-llm-cobol-discovery"
---

# Reemplazar NON_PERSISTENCE_VERB_NAMES por clasificación LLM gobernada de verbos CICS/SQL

## Description

Parte de epic-llm-cobol-discovery.

## Objetivo
Reemplazar la lista hardcodeada `NON_PERSISTENCE_VERB_NAMES`/`isFileControlVerbStub` (usada para decidir si un nodo "repository" descubierto es en realidad un stub de verbo CICS/SQL sin forma real de persistencia, o un repositorio genuino) por una clasificación vía prompt LLM gobernado, con el código actual como fallback determinista si el LLM no está disponible.

## Contexto técnico necesario
- Archivo: `renovatio-vscode-extension/src/legacyExtension.js`, líneas ~2805-2821.
- Código actual:
  ```js
  const NON_PERSISTENCE_VERB_NAMES = new Set([
      // CICS FILE-CONTROL verbs
      'OPEN', 'CLOSE', 'READ', 'WRITE', 'REWRITE', 'DELETE', 'START', 'UNLOCK', 'BROWSE', 'ENDBR', 'RESETBR',
      // Embedded SQL statement verbs
      'SELECT', 'INSERT', 'UPDATE', 'FETCH', 'SET', 'DECLARE', 'EXEC'
  ]);

  function isFileControlVerbStub(node) {
    if (hasPersistenceShape(node)) return false;
    const name = String(node?.name ?? node?.id ?? '').trim().toUpperCase();
    return !name || NON_PERSISTENCE_VERB_NAMES.has(name);
  }
  ```
  Se usa en `persistenceNodes(discovery, sqlFacts)` (misma zona del archivo) para filtrar candidatos a nodo de persistencia: `asArray(discovery.repositories).filter(node => !isFileControlVerbStub(node))`.
- El comentario justo arriba de la lista (línea ~2800-2809, revisar en el archivo) explica el motivo original: un intento previo de filtrar por "tiene forma de persistencia" (`hasPersistenceShape`) descartaba de más, tapando repositorios de archivo genuinos que todavía no tenían columnas resueltas — por eso se volvió a una lista de nombres de verbo conocidos como filtro adicional más conservador. Esa razón sigue siendo válida y hay que preservarla en el diseño del reemplazo (no volver a `hasPersistenceShape` solo).
- Esto es código del lado del **cliente** (VS Code extension, JavaScript/TypeScript, sin acceso directo a `renovatio-llm` que es Java) — a diferencia del resto de este epic y del epic de domain-entities, este punto requiere decidir CÓMO el frontend accedería a una clasificación LLM: lo más consistente con la arquitectura actual es que la clasificación se mueva al **backend** (Java, `renovatio-api`/`renovatio-provider-cobol`) como parte del discovery, y que el resultado ya venga clasificado en la respuesta que el frontend consume — no que el frontend llame al LLM directamente. Investigar primero dónde se genera el `discovery.repositories` que el frontend recibe (buscar el endpoint/servicio backend que arma esa estructura) antes de decidir el punto de inserción.
- Precedente de patrón a seguir: mismo que `epic-llm-domain-entities` — prompt YAML + JSON schema + fallback + validador de grounding (acá el "hecho" a verificar es que el verbo clasificado exista realmente en el código fuente analizado, para que el LLM no pueda inventar una clasificación sobre un verbo que no aparece).

## Tareas
1. **Confirmar el diagnóstico antes de implementar**: localizar en el backend (`renovatio-api`/`renovatio-provider-cobol`) dónde se construye la estructura `discovery.repositories`/`discovery.recordNodes` que el frontend consume, para decidir si el reemplazo debe vivir ahí (recomendado) o si de verdad hace falta mantenerlo en el frontend.
2. Diseñar el prompt (`cobol.cics-verb-classification.v1` o nombre similar): input = verbo + contexto mínimo (statement completo, tipo de recurso si se conoce); output = clasificación (`PERSISTENCE_BOUNDARY` vs. `TRANSACTION_CONTROL_STUB`) + confianza + rationale.
3. Implementar prompt YAML + JSON schema + fallback siguiendo el patrón de `cobol.domain.entities.v1` (ver `renovatio-llm/src/main/resources/prompts/cobol.domain.entities.v1.yaml` como referencia directa de formato).
4. Implementar el validador de grounding (puede compartir infraestructura con el validador de la issue `domain-entities-grounding-validator` del otro epic si ya existe para cuando se implemente esta issue — revisar orden de ejecución real entre epics).
5. Wiring backend: clasificar cada verbo candidato antes de que la respuesta llegue al frontend, dejando `NON_PERSISTENCE_VERB_NAMES`/`isFileControlVerbStub` como fallback determinista si el backend no clasifica (por retrocompatibilidad con el filtro actual del lado del cliente, hasta que todo el flujo esté migrado).
6. Tests: casos con verbos ya conocidos (deben clasificar igual que la lista actual, para no regresionar), y al menos un verbo CICS no incluido hoy en la lista (ej. un verbo de un dialecto distinto) para demostrar que generaliza donde la lista fija no lo haría.

## Criterios de aceptación
- [ ] Diagnóstico de dónde vive `discovery.repositories` en el backend confirmado y documentado en el PR.
- [ ] Los verbos ya presentes en `NON_PERSISTENCE_VERB_NAMES` siguen clasificando igual (sin regresión visible en el frontend).
- [ ] Al menos un verbo CICS FILE-CONTROL no incluido en la lista original se clasifica correctamente sin agregarlo a mano a ninguna lista.
- [ ] Si el LLM no está disponible, el sistema degrada limpiamente al comportamiento actual de `isFileControlVerbStub`.

## Evidencia a dejar en el PR
- Tests de clasificación (casos conocidos + caso nuevo no hardcodeado).
- Confirmación de que el fallback determinista funciona (test con LLM deshabilitado/fake que falla).

## Acceptance criteria

- [ ] **discovery-source-found:** Punto backend donde se arma discovery.repositories identificado; stages: none
- [ ] **no-regression-known-verbs:** Verbos ya en la lista actual clasifican igual (sin regresión); stages: none
- [ ] **generalizes-new-verb:** Al menos un verbo CICS no incluido en la lista original clasifica correctamente; stages: none
- [ ] **clean-fallback:** Si el LLM no está disponible, degrada al comportamiento actual de isFileControlVerbStub; stages: none

## Required artifacts

- none
