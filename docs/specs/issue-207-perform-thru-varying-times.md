# Especificación · #207 — `PERFORM` THRU / VARYING / UNTIL / TIMES y extracción de párrafos

- **Swarm:** `issue-207-perform-thru-varying-times` (047)
- **Work item:** `perform-thru-varying-times`
- **Issue:** https://github.com/Modern-Ash/renovatio/issues/207
- **Método:** spec-driven

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

## Aceptación

- Fixtures de caracterización para `PERFORM simple`, `THRU`, `n TIMES`, `UNTIL` (BEFORE y AFTER)
  y `VARYING` con `AFTER`, con Java esperado y comportamiento.
- El Java generado tiene un método por párrafo referenciado, sin duplicación de cuerpo.
- `perform-simple-nested` (fixture existente) queda verde observando `MAIN → OUTER-PARA → INNER-PARA`.
- Un programa del corpus CardDemo con `PERFORM VARYING` produce Java que compila; el pase de
  golden-master se valida en #216, último del lote.
- Recursión detectada → `ManualActionItem`, sin bucle infinito en generación.
- Permanecen verdes `renovatio-cobol-ir`, `cobol-openrewrite-recipes` y `renovatio-provider-cobol`.

## Fuera de alcance

- Fidelidad completa de `GO TO` y saltos no estructurados (#206 residual).
- `PERFORM` dependiente de `TIMES`/`VARYING` con expresiones complejas se traduce a nivel literal
  de operandos; condiciones compuestas siguen el traductor de condiciones existente.
- Golden-master E2E sobre programas completos CardDemo (#216).