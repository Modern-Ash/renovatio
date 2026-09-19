---
schema: "agora/work/v1"
id: "cics-dynamic-filename-resolution"
swarm: "llm-cobol-discovery-epic"
title: "Resolver nombre de archivo din\u00e1mico en CICS (FILE(LIT-ACCTFILENAME) y variantes)"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"origin-traced":"Origen exacto del nodo FILE(LIT-ACCTFILENAME) identificado","approach-decided":"Decisi\u00f3n determinista/LLM/h\u00edbrida documentada y justificada","real-case-resolved":"Al menos un caso real de CardDemo resuelve al nombre de dataset real","orphan-node-gone":"El nodo hu\u00e9rfano correspondiente desaparece del modelo de dominio de CardDemo"}
satisfied-criteria: []
criterion-statuses: {"origin-traced":[],"approach-decided":[],"real-case-resolved":[],"orphan-node-gone":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-cobol-discovery-epic/epic-llm-cobol-discovery"
---

# Resolver nombre de archivo dinámico en CICS (FILE(LIT-ACCTFILENAME) y variantes)

## Description

Parte de epic-llm-cobol-discovery.

## Objetivo
Resolver el nombre de archivo/dataset real detrás de una referencia CICS dinámica (ej. `EXEC CICS READ FILE(LIT-ACCTFILENAME) ...`, donde `LIT-ACCTFILENAME` es una variable cuyo valor literal se asigna en otro punto del programa, no el nombre del archivo en sí) — hoy este patrón produce un nodo de repositorio huérfano con el texto crudo `FILE(LIT-ACCTFILENAME)` como nombre en vez del dataset real, observado directamente en el modelo de dominio generado para AWS CardDemo.

## Contexto técnico necesario
- **Confirmado en esta sesión**: `CobolSemanticProjector.projectCics(...)` (`renovatio-provider-cobol/src/main/java/org/modernash/renovatio/provider/cobol/translation/CobolSemanticProjector.java`, líneas ~141-152) solo captura el **verbo** CICS vía `CICS_COMMAND = Pattern.compile("EXEC\\s+CICS\\s+([A-Z0-9-]+)", ...)` (línea 46) y crea un `IoOperation` con `resourceReference = Optional.empty()` siempre — el argumento `FILE(...)` del comando CICS **nunca se parsea en este método**.
- **No confirmado con certeza en esta sesión**: el punto exacto del código donde se genera el nodo observado `FILE(LIT-ACCTFILENAME)` en el modelo de dominio real de CardDemo — una búsqueda de `"FILE("` en `renovatio-cobol-ir/src/main/java` y `renovatio-provider-cobol/src/main/java` no encontró coincidencias directas, así que probablemente el patrón `FILE(LIT-ACCTFILENAME)` entra al modelo a través de otro extractor genérico (posiblemente el parser de `FileOperationStatement`/`parseFileName` en `SimpleCobolIrParser.java`, tratando el argumento parentizado completo como si fuera un nombre de archivo literal) o como una construcción no clasificada que terminó promovida de alguna forma. **La primera tarea de esta issue es rastrear con certeza el origen exacto antes de diseñar la solución.**
- El patrón CICS real en CardDemo (buscar en `/home/faguero/.renovatio/workspaces/aws-mainframe-modernization-carddemo/`) es: el programa declara una variable (ej. `LIT-ACCTFILENAME`) con un `VALUE 'ACCTDAT'` (o similar) en WORKING-STORAGE, y la referencia dentro de `EXEC CICS ... FILE(LIT-ACCTFILENAME) ...` — resolver el nombre real del dataset requiere: (a) parsear el argumento `FILE(...)` del comando CICS para extraer el nombre de la variable, y (b) resolver el valor de esa variable buscando su cláusula `VALUE` en la declaración de datos — es el mismo tipo de razonamiento de flujo de datos que motivó `SemanticProgram.FieldFlow` (ver `epic-llm-domain-entities`), pero para asignación de literal en vez de MOVE entre dos campos.
- Comparar con el enfoque puramente determinista: sería posible resolver esto sin LLM (extraer el argumento `FILE(...)`, buscar la declaración `01/05 <nombre> ... VALUE '<literal>'` correspondiente) — **evaluar explícitamente si esto es un caso mejor resuelto de forma determinista** (es una búsqueda de asignación de literal, razonablemente mecánica) antes de asumir que hace falta LLM; si la resolución determinista simple cubre la mayoría de los casos reales, preferirla y reservar LLM solo para los casos donde el valor no es un literal directo sino que depende de lógica condicional (ej. el nombre de archivo varía según una condición de negocio).

## Tareas
1. **Rastrear el origen exacto** del nodo `FILE(LIT-ACCTFILENAME)` observado en el modelo de dominio real de CardDemo — reproducir analizando el proyecto real y ubicar en qué método/clase se genera ese string tal cual.
2. Documentar la decisión: resolver de forma determinista (buscar la declaración `VALUE` de la variable referenciada) vs. resolver vía LLM gobernado (para casos donde el valor no es un literal directo) — es razonable proponer una solución híbrida (determinista primero, LLM como fallback para los casos ambiguos que la resolución mecánica no pueda cerrar).
3. Implementar el parseo del argumento `FILE(...)` (y variantes CICS equivalentes, ej. `DATASET(...)` en otros verbos) en `projectCics` o el extractor que corresponda según el diagnóstico del punto 1.
4. Implementar la resolución del valor (determinista y/o vía LLM según la decisión del punto 2), poblando `resourceReference` del `IoOperation` con el nombre real del dataset en vez de la expresión cruda.
5. Si se implementa la vía LLM: seguir el patrón de prompt/schema/fallback/validador de grounding del resto del epic.
6. Tests: al menos un caso real de CardDemo (`FILE(LIT-ACCTFILENAME)` u otro similar encontrado en el código real) resolviendo al nombre de dataset correcto.

## Criterios de aceptación
- [ ] Origen exacto del nodo `FILE(LIT-ACCTFILENAME)` identificado y documentado.
- [ ] Decisión determinista-vs-LLM (o híbrida) documentada con su justificación.
- [ ] Al menos un caso real de CardDemo resuelve al nombre de dataset real en vez de quedar como `FILE(<variable>)` crudo.
- [ ] El nodo huérfano correspondiente desaparece del modelo de dominio de CardDemo tras el fix (verificar re-analizando el proyecto real, recordando borrar la fila vieja de `project_domain_model_versions` en `renovatio-api/data/renovatio.db` antes de re-analizar, por la semántica de merge-no-reemplazo del backend).

## Evidencia a dejar en el PR
- Confirmación del origen exacto del nodo huérfano (antes del fix).
- Test(s) con el caso real de CardDemo resolviendo correctamente.
- Verificación en el modelo de dominio real post-fix (captura o export JSON mostrando el nodo ya no huérfano).

## Acceptance criteria

- [ ] **origin-traced:** Origen exacto del nodo FILE(LIT-ACCTFILENAME) identificado; stages: none
- [ ] **approach-decided:** Decisión determinista/LLM/híbrida documentada y justificada; stages: none
- [ ] **real-case-resolved:** Al menos un caso real de CardDemo resuelve al nombre de dataset real; stages: none
- [ ] **orphan-node-gone:** El nodo huérfano correspondiente desaparece del modelo de dominio de CardDemo; stages: none

## Required artifacts

- none
