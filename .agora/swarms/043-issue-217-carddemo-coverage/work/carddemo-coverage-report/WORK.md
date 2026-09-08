---
schema: "agora/work/v1"
id: "carddemo-coverage-report"
swarm: "issue-217-carddemo-coverage"
title: "Reporte de cobertura del pipeline sobre el corpus CardDemo (#217)"
state: "verifying"
revision: 2
operational-status: "revalidation"
status-reason: "Authorized owner follow-up: correct run-specific temporary paths in generated reports and revalidate tracking-json determinism."
status-by: "project:owner"
status-at: "2026-09-08T13:24:58.719904Z"
acceptance-criteria: {"corpus-vendored":"Las fuentes COBOL/copybook/JCL de CardDemo app quedan versionadas en el repo bajo una ruta de test dedicada, con su licencia Apache-2.0 y procedencia.","coverage-tool":"Existe una herramienta (test @Tag o clase ejecutable) que corre el pipeline por cada programa del corpus y registra: parseo ok/ko, generaci\u00f3n Java ok/ko, compilaci\u00f3n javac ok/ko, y verbos/constructos no soportados con su frecuencia.","aggregate-report":"Se genera docs/reports/carddemo-coverage.md con N/total programas que parsean, M/total que compilan, top de constructos no soportados por frecuencia y desglose por subsistema (batch/CICS/DB2/IMS+MQ).","tracking-json":"Se commitea un JSON de cobertura (docs/reports/carddemo-coverage.json) con la misma data, estable entre corridas para comparar tendencia.","e2e-candidates":"El reporte identifica expl\u00edcitamente los 3 programas batch m\u00e1s simples sin CICS/DB2 como candidatos para el E2E de #216.","reproducible-ci":"Un comando documentado en README (o docs) ejecuta el reporte; corre en CI (aunque sea nightly) sin depender de rutas absolutas ni red.","no-engine-change":"git diff no toca el traductor COBOL->Java (PopulateCobolProcessRecipe, JavaGenerationService salvo lo m\u00ednimo para exponer m\u00e9tricas); el m\u00f3dulo renovatio-provider-cobol sigue 114/114."}
satisfied-criteria: []
criterion-statuses: {"corpus-vendored":[],"coverage-tool":[],"aggregate-report":[],"tracking-json":[],"e2e-candidates":[],"reproducible-ci":[],"no-engine-change":[]}
required-artifacts: ["spec","implementation-plan","coverage-report","verification-report"]
child-work-refs: []
budget-limits: null
---

# Reporte de cobertura del pipeline sobre el corpus CardDemo (#217)

## Description

Herramienta reproducible que corre parse -> IR -> emisión Java -> javac sobre todo el COBOL de CardDemo y publica un reporte agregado (Markdown + JSON versionado). Vendoriza el corpus CardDemo app en el repo. No cambia el motor de traducción; sólo mide.

## Acceptance criteria

- [ ] **corpus-vendored:** Las fuentes COBOL/copybook/JCL de CardDemo app quedan versionadas en el repo bajo una ruta de test dedicada, con su licencia Apache-2.0 y procedencia.; stages: none
- [ ] **coverage-tool:** Existe una herramienta (test @Tag o clase ejecutable) que corre el pipeline por cada programa del corpus y registra: parseo ok/ko, generación Java ok/ko, compilación javac ok/ko, y verbos/constructos no soportados con su frecuencia.; stages: none
- [ ] **aggregate-report:** Se genera docs/reports/carddemo-coverage.md con N/total programas que parsean, M/total que compilan, top de constructos no soportados por frecuencia y desglose por subsistema (batch/CICS/DB2/IMS+MQ).; stages: none
- [ ] **tracking-json:** Se commitea un JSON de cobertura (docs/reports/carddemo-coverage.json) con la misma data, estable entre corridas para comparar tendencia.; stages: none
- [ ] **e2e-candidates:** El reporte identifica explícitamente los 3 programas batch más simples sin CICS/DB2 como candidatos para el E2E de #216.; stages: none
- [ ] **reproducible-ci:** Un comando documentado en README (o docs) ejecuta el reporte; corre en CI (aunque sea nightly) sin depender de rutas absolutas ni red.; stages: none
- [ ] **no-engine-change:** git diff no toca el traductor COBOL->Java (PopulateCobolProcessRecipe, JavaGenerationService salvo lo mínimo para exponer métricas); el módulo renovatio-provider-cobol sigue 114/114.; stages: none

## Required artifacts

- spec
- implementation-plan
- coverage-report
- verification-report
