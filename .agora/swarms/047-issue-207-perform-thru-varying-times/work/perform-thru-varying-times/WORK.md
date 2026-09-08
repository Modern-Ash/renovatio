---
schema: "agora/work/v1"
id: "perform-thru-varying-times"
swarm: "issue-207-perform-thru-varying-times"
title: "PERFORM THRU / VARYING / UNTIL / TIMES y extracci\u00f3n de p\u00e1rrafos a m\u00e9todos (#207)"
state: "verifying"
revision: 2
operational-status: "revalidation"
status-reason: "Persist final evidence and publish the accepted implementation."
status-by: "project:owner"
status-at: "2026-09-08T22:12:43.749241Z"
acceptance-criteria: {"ir-representation":"PERFORM THRU/VARYING/UNTIL/TIMES se representan con nodos IR expl\u00edcitos (paragraph, throughParagraph, varyingVariable/from/by, untilCondition, timesCount) con validaci\u00f3n de esquema y constructor.","method-extraction":"El Java generado tiene un m\u00e9todo privado por p\u00e1rrafo referenciado por PERFORM, sin duplicaci\u00f3n de cuerpo ni inlineado en el punto de llamada.","perform-thru":"PERFORM p THRU q ejecuta la secuencia de p\u00e1rrafos p..q en orden de aparici\u00f3n; si el rango cruza fall-through no lineal se emite un action item.","looping-variants":"PERFORM n TIMES emite for (int i=0;i<n;i++); UNTIL emite while con soporte TEST BEFORE/AFTER (default BEFORE); VARYING x FROM a BY b UNTIL cond emite for con variable de control, incluido AFTER (loops anidados); PERFORM inline (END-PERFORM) emite bloque.","recursion-guard":"Recursi\u00f3n o ciclos entre p\u00e1rrafos se detectan y emiten como ManualActionItem estable; la generaci\u00f3n nunca cuelga ni produce StackOverflowError.","traceability":"Cada m\u00e9todo extra\u00eddo conserva trazabilidad @GeneratedFrom con el p\u00e1rrafo COBOL y su rango de l\u00edneas.","characterization":"Fixtures de caracterizaci\u00f3n cubren PERFORM simple, THRU, n TIMES, UNTIL (BEFORE y AFTER) y VARYING con AFTER, con Java esperado y comportamiento.","nested-green":"El fixture existente perform-simple-nested queda verde tras el cambio a m\u00e9todos.","carddemo-compiles":"Un programa del corpus CardDemo con PERFORM VARYING produce Java que compila.","regression-green":"IR, OpenRewrite y provider-cobol quedan verdes."}
satisfied-criteria: ["ir-representation","method-extraction","perform-thru","looping-variants","recursion-guard","traceability","characterization","nested-green","carddemo-compiles","regression-green"]
criterion-statuses: {"ir-representation":["specified","planned","implemented","verified","accepted"],"method-extraction":["specified","planned","implemented","verified","accepted"],"perform-thru":["specified","planned","implemented","verified","accepted"],"looping-variants":["specified","planned","implemented","verified","accepted"],"recursion-guard":["specified","planned","implemented","verified","accepted"],"traceability":["specified","planned","implemented","verified","accepted"],"characterization":["specified","planned","implemented","verified","accepted"],"nested-green":["specified","planned","implemented","verified","accepted"],"carddemo-compiles":["specified","planned","implemented","verified","accepted"],"regression-green":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","coverage-report"]
child-work-refs: []
budget-limits: null
---

# PERFORM THRU / VARYING / UNTIL / TIMES y extracción de párrafos a métodos (#207)

## Description

Traducir PERFORM en todas sus formas (THRU / n TIMES / UNTIL / VARYING FROM BY UNTIL) con semántica correcta y generar Java idiomático: cada párrafo referenciado se extrae como método privado, sin inlineado duplicado. Mantener trazabilidad @GeneratedFrom por método y detectar recursión como ManualActionItem.

## Acceptance criteria

- [x] **ir-representation:** PERFORM THRU/VARYING/UNTIL/TIMES se representan con nodos IR explícitos (paragraph, throughParagraph, varyingVariable/from/by, untilCondition, timesCount) con validación de esquema y constructor.; stages: specified, planned, implemented, verified, accepted
- [x] **method-extraction:** El Java generado tiene un método privado por párrafo referenciado por PERFORM, sin duplicación de cuerpo ni inlineado en el punto de llamada.; stages: specified, planned, implemented, verified, accepted
- [x] **perform-thru:** PERFORM p THRU q ejecuta la secuencia de párrafos p..q en orden de aparición; si el rango cruza fall-through no lineal se emite un action item.; stages: specified, planned, implemented, verified, accepted
- [x] **looping-variants:** PERFORM n TIMES emite for (int i=0;i<n;i++); UNTIL emite while con soporte TEST BEFORE/AFTER (default BEFORE); VARYING x FROM a BY b UNTIL cond emite for con variable de control, incluido AFTER (loops anidados); PERFORM inline (END-PERFORM) emite bloque.; stages: specified, planned, implemented, verified, accepted
- [x] **recursion-guard:** Recursión o ciclos entre párrafos se detectan y emiten como ManualActionItem estable; la generación nunca cuelga ni produce StackOverflowError.; stages: specified, planned, implemented, verified, accepted
- [x] **traceability:** Cada método extraído conserva trazabilidad @GeneratedFrom con el párrafo COBOL y su rango de líneas.; stages: specified, planned, implemented, verified, accepted
- [x] **characterization:** Fixtures de caracterización cubren PERFORM simple, THRU, n TIMES, UNTIL (BEFORE y AFTER) y VARYING con AFTER, con Java esperado y comportamiento.; stages: specified, planned, implemented, verified, accepted
- [x] **nested-green:** El fixture existente perform-simple-nested queda verde tras el cambio a métodos.; stages: specified, planned, implemented, verified, accepted
- [x] **carddemo-compiles:** Un programa del corpus CardDemo con PERFORM VARYING produce Java que compila.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-green:** IR, OpenRewrite y provider-cobol quedan verdes.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- coverage-report
