# Especificación · #207 — `PERFORM` THRU / VARYING / UNTIL / TIMES y extracción de párrafos

- **Swarm:** `issue-207-perform-thru-varying-times` (047)
- **Work item:** `perform-thru-varying-times`
- **Issue:** https://github.com/Modern-Ash/renovatio/issues/207
- **Método:** spec-driven
- **Estado:** `verifying` — 10/10 criterios satisfechos
- **Revisión:** 0003
- **Artefactos que se mantienen en sync:** este documento,
  `docs/plans/issue-207-perform-thru-varying-times.md`,
  `.../work/perform-thru-varying-times/WORK.md` (criterios/estados),
  `renovatio-cobol-ir/src/main/resources/schema/cobol-ir.v1.schema.json`,
  `docs/reports/issue-207-perform-thru-varying-times-verification.md`,
  `docs/reports/carddemo-coverage.{md,json}`.
- **Golden-master de aceptación:** `docs/reports/carddemo-coverage.md` (+ `.json`),
  regenerado por `CardDemoCoverageReportTest`; objetivo del ciclo 44 parse / 44 emit /
  ≥1 programa con `VARYING` que compila. El pase E2E completo se valida en #216.

## Objetivo

Traducir `PERFORM` en todas sus formas con semántica correcta y generar Java idiomático: cada
párrafo referenciado se extrae como **método privado**, no como cuerpo inlineado repetido. El
guardrail de caracterización `perform-simple-nested` exige que la ejecución generada observe el
orden `MAIN → OUTER-PARA → INNER-PARA`, lo cual sólo es demostrable con llamadas de método reales.

## Contrato

- El IR representa `PERFORM` con todos sus modificadores: `paragraph`, `throughParagraph`,
  `varyingVariable`/`varyingFrom`/`varyingBy`, `untilCondition`, `timesCount`, `testAfter`
  (default `BEFORE`) y `inlineBody` (`PERFORM ... END-PERFORM`).
- `PERFORM <p>` → llamada al método privado extraído `performP(input, out)`.
- `PERFORM <p> THRU <q>` → el llamador invoca la secuencia ordenada de llamadas `performP ..
  performQ` (cada párrafo del rango es un método propio; el cuerpo nunca se duplica).
- `PERFORM ... n TIMES` → `for (int i = 0; i < n; i++) { ... }`.
- `PERFORM ... UNTIL cond` → `while (!(cond)) { ... }`, con `TEST AFTER` → `do { ... } while (!(cond))`;
  el default es `TEST BEFORE`.
- `PERFORM ... VARYING x FROM a BY b UNTIL cond` → bucle `for` con variable de control local
  (`for (int x = a; !(cd); x += b)`), soportando `AFTER` como bucles anidados.
- `PERFORM ... END-PERFORM` (inline, bloque multilínea) → bloque renderizado en el punto de llamada;
  no hay párrafo destino.
- Recursión o ciclos entre párrafos → `ManualActionItem` estable + comentario en el Java; la
  generación nunca cuelga ni produce `StackOverflowError`.
- Cada método extraído conserva trazabilidad `@GeneratedFrom` con el párrafo COBOL y el rango de
  líneas; la firma extraída es `private void performX(Dto input, Dto out)`, coherente con el resto
  del emisor.
- **Guardrail de datos no modelados:** una referencia dentro de una sentencia que no corresponde
  a un item del IR ni a una variable asignada degrada la **sentencia completa** a
  `// COBOL not translated: <name> (data item not modeled)`. El LST resultante es siempre válido
  (sin NPE al formatear) y la generación permanece estable — es el mecanismo que evita el crash de
  COPAUA0C (emisión 36→44) y los setters con subíndices (`(1)(100500)`).
- **Normalización de subíndices:** la DTO aplana los campos `OCCURS` a escalares; setters/getters
  y resolución de referencias ignoran los subíndices `(n)`.
- **Formato fijo / CICS:** al extraer `EXEC CICS`, se descartan los números de secuencia de
  columnas 73-80 (que se capturaban como comandos fantasma `02980000`); los nombres de método
  derivados escapan keywords Java (`RETURN` → `return_`) y se prefijan con `tx` si no son
  identificadores válidos.

## Aceptación

- Fixtures de caracterización para `PERFORM simple`, `THRU`, `n TIMES`, `UNTIL` (BEFORE y AFTER)
  y `VARYING` con `AFTER`, con Java esperado y comportamiento.
- El Java generado tiene un método por párrafo referenciado, sin duplicación de cuerpo.
- `perform-simple-nested` (fixture existente) queda verde observando `MAIN → OUTER-PARA → INNER-PARA`.
- Al menos un programa del corpus CardDemo con `PERFORM VARYING` produce Java que compila; el
  reporte de cobertura hace fallar el test si este contrato deja de cumplirse.
- Recursión detectada → `ManualActionItem`, sin bucle infinito en generación.
- Permanecen verdes `renovatio-cobol-ir`, `cobol-openrewrite-recipes` y `renovatio-provider-cobol`.

## Fuera de alcance

- Fidelidad completa de `GO TO` y saltos no estructurados (#206 residual).
- `PERFORM` dependiente de `TIMES`/`VARYING` con expresiones complejas se traduce a nivel literal
  de operandos; condiciones compuestas siguen el traductor de condiciones existente.
- Golden-master E2E sobre programas completos CardDemo (#216).
- Robustez de la extracción léxica de WORKING-STORAGE para el DTO (`WS_FIELD_PATTERN`:
  `USAGE IS COMP-3.` en línea siguiente, cláusula `BINARY`, literales decimales) — follow-up
  preexistente, ajeno a PERFORM.

## Estado del cierre

Al cierre de la revisión 0003 la rama satisface **10/10** criterios. El harness de compilación
incluye `spring-web` con scope de test para compilar los controladores CICS generados y
`CardDemoCoverageReportTest` exige explícitamente que al menos un programa del corpus que contenga
`VARYING` compile.

### Resultado CardDemo

- Cobertura: **44/44 parse · 44/44 emit · 17/44 compila**.
- Compilan tres programas que contienen `PERFORM VARYING`: `COADM01C`, `COCRDLIC` y `COTRTLIC`.
- La lista `present` del reporte incluye ahora el token `VARYING`, de modo que el criterio se
  deriva del corpus analizado y no de una lista manual de programas.
- Los gaps de extracción DTO que aún afectan otros programas permanecen fuera del alcance de
  este issue y ya no bloquean su criterio de aceptación.

## Mapeo criterio → evidencia

Los 10 criterios formales viven en `WORK.md`. Estado y evidencia:

| Criterio | Estado | Evidencia |
|----------|--------|-----------|
| `ir-representation` | ✅ satisfecho | `PerformStatementParsingTest` (IR, 84 verdes); `cobol-ir.v1.schema.json` |
| `method-extraction` | ✅ satisfecho | `PopulateCobolProcessRecipeTest`; fixture `perform-simple-nested` |
| `perform-thru` | ✅ satisfecho | fixture `perform-thru`; `PopulateCobolProcessRecipeTest` |
| `looping-variants` | ✅ satisfecho | TIMES / UNTIL BEFORE·AFTER / VARYING 1-eje / VARYING…AFTER anidado / inline; `PerformStatementParsingTest.parsesPerformVaryingWithAfterAxes`, `PopulateCobolProcessRecipeTest.shouldRenderPerformVaryingAfterAsNestedLoops`, fixtures `perform-until-after` + `perform-varying-after` |
| `recursion-guard` | ✅ satisfecho | `PopulateCobolProcessRecipeTest` (ciclo → `ManualActionItem`, sin `StackOverflowError`) |
| `traceability` | ✅ satisfecho | `@GeneratedFrom` en métodos `performX`; `PopulateCobolProcessRecipeTest` |
| `characterization` | ✅ satisfecho | 7 fixtures: `perform-simple-nested`, `-thru`, `-times`, `-until`, `-until-after`, `-varying`, `-varying-after` (todos `SUPPORTED`, con `expected.java` que compila y comportamiento verificado) |
| `nested-green` | ✅ satisfecho | `CharacterizationFixtureContractTest` (`perform-simple-nested` verde) |
| `carddemo-compiles` | ✅ satisfecho | `CardDemoCoverageReportTest` exige ≥1 programa con `VARYING` compilable; reporte 44/44/17 con `COADM01C`, `COCRDLIC` y `COTRTLIC` verdes |
| `regression-green` | ✅ satisfecho | IR 84 · recipes 34 · provider-cobol verdes; `git diff --check` limpio |

Evidencia registrada en el work item:
`.../work/perform-thru-varying-times/evidence/evidence-000001` y `evidence-000002`, más el
informe `docs/reports/issue-207-perform-thru-varying-times-verification.md`.

## Historial de revisiones

- **0001** (2026-09-08) — Spec inicial + cierre de verificación: contrato de guardrail de datos
  no modelados, normalización de subíndices y saneo de formato fijo/CICS incorporados; 7/10
  criterios satisfechos; gaps `looping-variants` (AFTER), `characterization` (2 fixtures) y
  `carddemo-compiles` declarados como conocidos con sus follow-ups.
- **0002** (2026-09-08) — Cierre de `looping-variants` y `characterization`: `PERFORM VARYING
  ... AFTER` en IR + schema + parser + render como bucles anidados; fixtures `perform-until-after`
  y `perform-varying-after`. 9/10 criterios; sólo `carddemo-compiles` queda pendiente, diferido
  formalmente a #216.
- **0003** (2026-09-08) — Cierre de `carddemo-compiles`: `spring-web` disponible en el harness,
  detección explícita de `VARYING` en el reporte y aserción de aceptación. Cobertura 44/44/17;
  `COADM01C`, `COCRDLIC` y `COTRTLIC` compilan. 10/10 criterios satisfechos.
