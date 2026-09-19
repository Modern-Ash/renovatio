---
schema: "agora/work/v1"
id: "copy-replacing-support-eval"
swarm: "llm-cobol-discovery-epic"
title: "Evaluar soporte de COPY ... REPLACING ... (determinista vs LLM)"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"frequency-measured":"Frecuencia real de COPY REPLACING medida en proyectos de prueba disponibles","approach-decided":"Decisi\u00f3n int\u00e9rprete-determinista/LLM/no-soportado documentada y justificada","case-resolved":"Si se implementa, al menos un caso real o representativo expande correctamente","mechanically-verifiable":"Si es LLM, la sustituci\u00f3n final es verificable mec\u00e1nicamente antes de aplicarse"}
satisfied-criteria: []
criterion-statuses: {"frequency-measured":[],"approach-decided":[],"case-resolved":[],"mechanically-verifiable":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-cobol-discovery-epic/epic-llm-cobol-discovery"
---

# Evaluar soporte de COPY ... REPLACING ... (determinista vs LLM)

## Description

Parte de epic-llm-cobol-discovery. El más riesgoso semánticamente del epic — priorizarlo último, después de validar el patrón con las issues más chicas.

## Objetivo
Evaluar si conviene dar soporte a copybooks incluidos con cláusula `COPY ... REPLACING ...` (hoy deliberadamente NO expandidos por `SimpleCobolIrParser`, dejados como texto inerte) usando un LLM para aplicar la sustitución de tokens, en vez de escribir un intérprete determinista de la sintaxis `REPLACING` de COBOL.

## Contexto técnico necesario
- Archivo: `renovatio-cobol-ir/src/main/java/org/modernash/renovatio/cobol/ir/parser/SimpleCobolIrParser.java`.
- `COPY_PATTERN` (agregada en la sesión que originó `epic-llm-domain-entities`, ver comentario adjunto en el código):
  ```java
  // A plain "COPY <member>." with nothing else between the name and its
  // terminating period — deliberately conservative. A COPY with a
  // REPLACING clause changes the copied text and this simple textual
  // splice can't honor that safely, so such statements are left
  // unexpanded rather than risk inlining the wrong content; better to
  // miss a field than silently mis-substitute one.
  private static final Pattern COPY_PATTERN = Pattern.compile(
          "(?m)^\\s*COPY\\s+([A-Za-z0-9-]+)\\s*\\.", Pattern.CASE_INSENSITIVE);
  ```
  El patrón solo matchea `COPY <nombre>.` con el punto inmediatamente después del nombre — cualquier `COPY X REPLACING ==A== BY ==B==.` no matchea y por lo tanto NO se expande en absoluto, dejando ese copybook completamente invisible para el resto del pipeline (ningún campo de ese copybook se agrega a `dataItems`).
- Método de expansión: `expandCopyStatements(String source, Path baseFile, int depth, Deque<String> inProgress)` (agregado en la misma sesión) — hace splice textual recursivo con cache de índice de copybooks por workspace (`copybookIndex`/`buildCopybookIndex`/`workspaceRoot`). Cualquier solución para `REPLACING` tendría que integrarse en este mismo flujo (antes o durante el splice).
- La sintaxis real de `REPLACING` en COBOL permite reemplazar tokens/pseudo-texto (`==texto==`) o identificadores completos dentro del copybook copiado, potencialmente varias reglas en una sola cláusula (`REPLACING ==A== BY ==B== ==C== BY ==D==`) — es una sustitución de texto mecánica pero con reglas de tokenización COBOL no triviales (bordes de palabra, pseudo-texto delimitado por `==`, etc.).
- **Evaluar primero qué tan frecuente es este patrón en proyectos reales** antes de invertir esfuerzo — en el workspace de AWS CardDemo usado en las sesiones anteriores, verificar cuántos `COPY ... REPLACING` existen realmente (`grep -rn "COPY.*REPLACING" <workspace>/app/cbl/`) para dimensionar el impacto real; si es un patrón raro en la práctica, puede no justificar ni siquiera la vía LLM, y alcanzaría con seguir dejándolo sin expandir (documentando la limitación) o implementar un intérprete determinista simple si los casos reales son todos de la forma más básica (una sola regla `REPLACING ==A== BY ==B==`).

## Tareas
1. **Medir el impacto real**: contar ocurrencias de `COPY ... REPLACING` en el workspace de CardDemo y, si hay otros proyectos de prueba disponibles en el repo, en esos también. Documentar el resultado.
2. Si el impacto es bajo (pocos casos, todos de forma simple): implementar un intérprete determinista acotado (una o más reglas `REPLACING ==X== BY ==Y==` con sustitución literal de token) — más simple, más confiable, y sin costo de LLM, para el subconjunto de sintaxis realmente encontrado.
3. Si el impacto es alto o la sintaxis encontrada es más compleja de lo que un intérprete simple puede cubrir con confianza: diseñar la vía LLM — el prompt recibiría el texto del copybook + la cláusula `REPLACING` cruda, y debería devolver el texto ya sustituido (o, más seguro para el validador de grounding, una lista estructurada de las sustituciones a aplicar en vez de el texto completo regenerado, para poder verificar mecánicamente que cada sustitución es coherente con la cláusula original antes de aplicarla textualmente con código propio — preferir esta opción para minimizar el riesgo de que el LLM "reescriba" contenido no relacionado con la sustitución).
4. Cualquiera sea la vía elegida: test con al menos un caso real de `COPY ... REPLACING` encontrado en el paso 1 (o uno sintético representativo si no hay casos reales), verificando que el campo resultante después de la sustitución aparece correctamente en `dataItems`.

## Criterios de aceptación
- [ ] Medición documentada de la frecuencia real de `COPY ... REPLACING` en proyectos de prueba disponibles.
- [ ] Decisión documentada y justificada: intérprete determinista simple vs. vía LLM vs. seguir sin soportarlo (las tres son resultados válidos según lo que arroje la medición).
- [ ] Si se implementa soporte: al menos un caso real o representativo expande correctamente con la sustitución aplicada.
- [ ] Si se implementa la vía LLM: el mecanismo de aplicar la sustitución final es verificable mecánicamente (no se confía ciegamente en texto regenerado completo por el LLM sin poder validar que solo cambió lo que la cláusula `REPLACING` indicaba).

## Evidencia a dejar en el PR
- Conteo/medición de ocurrencias reales de `COPY ... REPLACING`.
- Test(s) del caso elegido, con el campo resultante verificado en `dataItems`.

## Acceptance criteria

- [ ] **frequency-measured:** Frecuencia real de COPY REPLACING medida en proyectos de prueba disponibles; stages: none
- [ ] **approach-decided:** Decisión intérprete-determinista/LLM/no-soportado documentada y justificada; stages: none
- [ ] **case-resolved:** Si se implementa, al menos un caso real o representativo expande correctamente; stages: none
- [ ] **mechanically-verifiable:** Si es LLM, la sustitución final es verificable mecánicamente antes de aplicarse; stages: none

## Required artifacts

- none
