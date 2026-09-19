---
schema: "agora/work/v1"
id: "control-break-detection-llm-eval"
swarm: "llm-cobol-discovery-epic"
title: "Evaluar reemplazo LLM de la detecci\u00f3n de patrones de control-break"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"current-heuristic-documented":"Heur\u00edstica actual documentada con ejemplos de acierto/fallo reales","decision-documented":"Decisi\u00f3n implementar-vs-mantener documentada y justificada","improvement-demonstrated":"Si se implementa, mejora demostrada sobre un caso de fallo conocido sin regresionar aciertos"}
satisfied-criteria: []
criterion-statuses: {"current-heuristic-documented":[],"decision-documented":[],"improvement-demonstrated":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-cobol-discovery-epic/epic-llm-cobol-discovery"
---

# Evaluar reemplazo LLM de la detección de patrones de control-break

## Description

Parte de epic-llm-cobol-discovery.

## Objetivo
Evaluar y, si conviene, reemplazar (parcial o totalmente) la detección de patrones de control-break basada en regex/heurística de `ControlBreakPatternDetector`/`BusinessLogicDecomposer` por una clasificación asistida por LLM gobernado, manteniendo el código actual como fallback determinista.

## Contexto técnico necesario
- Archivos: `renovatio-cobol-ir/src/main/java/org/modernash/renovatio/cobol/ir/parser/ControlBreakPatternDetector.java` (371 líneas — método principal `detectPatterns(CobolIntermediateModel model)` en línea 57, `detectBreakLevels(...)` en línea 169) y `renovatio-cobol-ir/src/main/java/org/modernash/renovatio/cobol/ir/parser/BusinessLogicDecomposer.java` (656 líneas). También `renovatio-provider-cobol/src/main/java/org/modernash/renovatio/provider/cobol/service/ControlBreakDecompositionService.java` (671 líneas) que consume la salida de ambos.
- Esta es, según el mapeo de complejidad hecho en la sesión que originó este epic, una de las áreas más grandes en líneas de código de todo `renovatio-cobol-ir`/`renovatio-provider-cobol` (top 3-4 en tamaño), y conceptualmente es "reconocer una forma de negocio en un patrón de código" (un control-break es: leer secuencialmente un archivo ordenado por una clave, detectar cuándo esa clave cambia entre un registro y el siguiente, y disparar lógica de "cierre de grupo" — ej. totales por cliente) — exactamente el tipo de reconocimiento de intención que un LLM suele hacer mejor que una cascada de regex sobre la estructura de párrafos.
- **A diferencia de la issue de verbos CICS (más chica y acotada), esta es una pieza central que alimenta decomposición de lógica de negocio para codegen** — un resultado incorrecto acá es más costoso de detectar tarde que en discovery de entidades. Por eso el orden sugerido del epic la pone después de las piezas más chicas (verbos CICS, nombre de archivo dinámico), para validar el patrón con menor riesgo primero.
- No se investigó en profundidad el comportamiento actual de `ControlBreakPatternDetector` en esta sesión — **la primera tarea de esta issue es understanding real, no asumir que ya se sabe cómo funciona.**

## Tareas
1. **Investigación/diagnóstico (obligatoria antes de cualquier cambio de código)**: leer `ControlBreakPatternDetector.detectPatterns`/`detectBreakLevels` y `BusinessLogicDecomposer` completos, entender qué señales usan hoy (nombres de párrafo, estructura de PERFORM, comparación de campos) para detectar un control-break, y documentar en el PR/issue un resumen de la heurística actual con ejemplos concretos de COBOL que la disparan y casos límite que probablemente se le escapan.
2. Correr la detección actual sobre el proyecto real AWS CardDemo (`/home/faguero/.renovatio/workspaces/aws-mainframe-modernization-carddemo/`, ya cargado en el backend local como project_id `873d7a49-b178-414c-a10c-f5364ff37f3f`) y sobre cualquier otro proyecto de prueba disponible en el repo (revisar `renovatio-provider-cobol/src/test/resources/` o similar para fixtures ya usados en tests de `ControlBreakPatternDetector`/`ControlBreakDecompositionService`), y catalogar casos donde la heurística actual falla o da falsos positivos/negativos — esto define si vale la pena el reemplazo LLM y sobre qué parte específica enfocar (puede que solo una sub-parte, como `detectBreakLevels`, sea el problema real).
3. Solo si el diagnóstico confirma que hay ganancia real: diseñar el prompt siguiendo el patrón de `cobol.domain.entities.v1` (facts deterministas de grounding = estructura de párrafo/PERFORM/comparaciones ya extraída, no el texto crudo completo) y su validador de grounding.
4. Si el diagnóstico concluye que NO conviene (ej. la heurística actual ya funciona bien, o el costo de LLM por proyecto es desproporcionado dado que corre sobre cada párrafo de cada programa), documentar esa decisión explícitamente en el issue/PR con la evidencia que la respalda — cerrar la issue con esa conclusión es un resultado válido, no un fracaso.

## Criterios de aceptación
- [ ] Documentación clara de cómo funciona la heurística actual, con ejemplos concretos (COBOL real) de aciertos y fallos conocidos.
- [ ] Decisión explícita y justificada: implementar reemplazo LLM (con su propio prompt/validador/fallback) o mantener la heurística actual tal cual (con la razón documentada).
- [ ] Si se implementa el reemplazo: tests que demuestren mejora sobre al menos un caso donde la heurística actual fallaba, sin regresionar los casos donde ya acertaba.

## Evidencia a dejar en el PR
- Catálogo de casos de acierto/fallo de la heurística actual sobre CardDemo y/o fixtures existentes.
- Si aplica, tests del nuevo prompt y comparación antes/después.

## Acceptance criteria

- [ ] **current-heuristic-documented:** Heurística actual documentada con ejemplos de acierto/fallo reales; stages: none
- [ ] **decision-documented:** Decisión implementar-vs-mantener documentada y justificada; stages: none
- [ ] **improvement-demonstrated:** Si se implementa, mejora demostrada sobre un caso de fallo conocido sin regresionar aciertos; stages: none

## Required artifacts

- none
