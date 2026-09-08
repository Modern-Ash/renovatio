---
schema: "agora/work/v1"
id: "display-continue-goback"
swarm: "issue-206-display-continue-goback"
title: "Traducir DISPLAY, CONTINUE, GOBACK, STOP RUN + no descartar en silencio (#206)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"display-rendered":"DISPLAY con literal y/o identificadores produce una llamada System.out.println equivalente (literal como String, identificadores como getters), verificado por fixture de caracterizacion.","continue-noop":"CONTINUE se traduce a un no-op explicito y no rompe el flujo generado.","goback-return":"GOBACK y STOP RUN se traducen a 'return <outputDto>;' en el punto donde aparecen.","no-silent-drop":"Toda sentencia de PROCEDURE DIVISION que el traductor aun no soporta aparece como comentario '// ... not translated' en el Java generado; ninguna se descarta en silencio y no se emite Java sintacticamente invalido para los programas del corpus CardDemo.","coverage-delta":"Se re-genera docs/reports/carddemo-coverage.{md,json}; DISPLAY, CONTINUE, GOBACK y STOP RUN salen de la lista de constructos no traducidos y el numero de programas que compilan no baja de 10.","regression-green":"renovatio-provider-cobol, cobol-openrewrite-recipes y renovatio-cobol-ir siguen 100% verdes; el gate jacoco de provider-cobol se mantiene."}
satisfied-criteria: ["display-rendered","continue-noop","goback-return","no-silent-drop","coverage-delta","regression-green"]
criterion-statuses: {"display-rendered":["specified","planned","implemented","verified","accepted"],"continue-noop":["specified","planned","implemented","verified","accepted"],"goback-return":["specified","planned","implemented","verified","accepted"],"no-silent-drop":["specified","planned","implemented","verified","accepted"],"coverage-delta":["specified","planned","implemented","verified","accepted"],"regression-green":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report"]
child-work-refs: []
budget-limits: null
---

# Traducir DISPLAY, CONTINUE, GOBACK, STOP RUN + no descartar en silencio (#206)

## Description

Primer ciclo de #206. El parser/recipe hoy descarta silenciosamente las sentencias que no reconoce. Este ciclo: (a) DISPLAY -> System.out.println con concatenacion de literal + getters; (b) CONTINUE -> no-op explicito; (c) GOBACK/STOP RUN -> return del DTO de salida; (d) cualquier otra sentencia de PROCEDURE DIVISION no reconocida -> comentario '// <verbo>: not translated yet', nunca se descarta ni se emite Java invalido. Sin tocar mapeo de tipos ni verbos que ya funcionan.

## Acceptance criteria

- [x] **display-rendered:** DISPLAY con literal y/o identificadores produce una llamada System.out.println equivalente (literal como String, identificadores como getters), verificado por fixture de caracterizacion.; stages: specified, planned, implemented, verified, accepted
- [x] **continue-noop:** CONTINUE se traduce a un no-op explicito y no rompe el flujo generado.; stages: specified, planned, implemented, verified, accepted
- [x] **goback-return:** GOBACK y STOP RUN se traducen a 'return <outputDto>;' en el punto donde aparecen.; stages: specified, planned, implemented, verified, accepted
- [x] **no-silent-drop:** Toda sentencia de PROCEDURE DIVISION que el traductor aun no soporta aparece como comentario '// ... not translated' en el Java generado; ninguna se descarta en silencio y no se emite Java sintacticamente invalido para los programas del corpus CardDemo.; stages: specified, planned, implemented, verified, accepted
- [x] **coverage-delta:** Se re-genera docs/reports/carddemo-coverage.{md,json}; DISPLAY, CONTINUE, GOBACK y STOP RUN salen de la lista de constructos no traducidos y el numero de programas que compilan no baja de 10.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-green:** renovatio-provider-cobol, cobol-openrewrite-recipes y renovatio-cobol-ir siguen 100% verdes; el gate jacoco de provider-cobol se mantiene.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
