# Informe de integración · #222 — baseline canónica

## Resultado

Se adopta `origin/main@6b46865171a795a18e84c570211c1dea14a7e6ea` como baseline. El
conjunto de ports funcionales de AC-01 es **vacío**: de los 85 commits de la línea F8, 49 están ya
presentes —tres de ellos con patch-id idéntico—, 15 pertenecen a diseños obsoletos, 16 implementan
un source replay no canónico y cinco introducirían nuevamente la regresión MVC/CICS. El único commit adicional de
workbench es metadata Agora de una línea histórica y también queda obsoleto.

La entrega contiene solamente esta decisión, la especificación/plan, el ADR, el reporte de tests,
el script reproducible y el work item Agora. No se mezcló ni se cherry-pickeó ninguna rama.

## Entradas y rollback

| Línea | Head | Merge-base con `main` | Tags remotos preservados |
|---|---|---|---|
| baseline | `6b46865171a795a18e84c570211c1dea14a7e6ea` | — | `ac01-pre-convergence-main-20260908` |
| F8 auditada | `71178f4451fa2616036d5c3a3967da519993ef77` | `c89d307c34fe16e885a0ba63591b974fbd00b870` | `ac01-pre-convergence-f8-20260908` |
| workbench-bootstrap | `7feaf7803db998c0c3fd87bf54a07396d78cce44` | `c89d307c34fe16e885a0ba63591b974fbd00b870` | `ac01-pre-convergence-workbench-20260908` |
| checkout de #217 | `2be7528e2df2d2c2083036f891a44a66ff1ddbd6` | el mismo SHA | incluido en `main`; 16 commits detrás |

Recuperación no destructiva:

```bash
git fetch origin --tags
git switch -c recovery/ac01-f8 ac01-pre-convergence-f8-20260908
git switch -c recovery/ac01-workbench ac01-pre-convergence-workbench-20260908
```

Para revertir la integración final se usa `git revert <merge-de-#222>` sobre una rama nueva. Los
tags no se mueven y no se usa `reset`, rebase destructivo ni force-push.

## Reproducción del inventario

El script no modifica refs ni el working tree:

```bash
git remote add workspace /ruta/al/checkout/original
git fetch workspace '+refs/heads/*:refs/remotes/workspace/*'
scripts/audit-branch-convergence.sh origin/main \
  workspace/agora/decision-engine-f8 \
  workspace/agora/renovatio-workbench-bootstrap \
  workspace/agora/issue-217-carddemo-coverage
```

Internamente ejecuta `rev-parse`, `merge-base`, `rev-list --left-right`,
`rev-list --cherry-pick`, `range-diff` y `git patch-id --stable`, y lista cada commit exclusivo con
su patch ID. Resultados principales:

| Candidato | Raw izquierda/derecha | Sin equivalentes izquierda/derecha | Range-diff `= / < / > / !` |
|---|---:|---:|---:|
| F8 | 85 / 108 | 82 / 105 | 3 / 82 / 83 / 0 |
| workbench-bootstrap | 86 / 108 | 83 / 105 | 3 / 83 / 83 / 0 |
| #217 | 0 / 16 | 0 / 16 | 0 / 0 / 16 / 0 |

Los tres pares exactos por `patch-id --stable` son:

| F8 | `main` | Patch ID |
|---|---|---|
| `1ef53de2` | `b0b77a6a` | `66a6c5e17af3f977f45b0e1c993a77107068652d` |
| `082aaeb3` | `498792d5` | `7bd2f15cbde1013181766b78655d68a0e19a848a` |
| `6d04e0be` | `c7e3c327` | `a8b26a185c76036abbc129eef8b1cf088a6e6a91` |

## Clasificación de los 85 commits F8

Owner de clasificación: `project:owner`. Owner de ejecución: el issue destino indicado.

| Serie (inclusive) | Cant. | Clasificación | Owner / destino | Justificación |
|---|---:|---|---|---|
| `1ef53de2`, `082aaeb3`, `6d04e0be` | 3 | ya-presente | AC-01 | equivalencia exacta de patch-id en `main` |
| `ec71c248..df251f29` | 14 | obsoleto | #225 | crea/proyecta un `ArchitectureModel` paralelo; #179 ya entregó edición/persistencia de dominio y #225 debe unificar modelos, no importar otro |
| `d19ec824..edc9218c` | 10 | ya-presente | #228 | reemplazado por `renovatio-shared/equivalence`, release gate, runbook y workbench de equivalencia integrados |
| `2d9bd60f..9cd58569` | 4 | ya-presente | #228 | `EquivalenceRunner`, `CommandObservationRunner` y harness canónico cubren el runner neutral/configurable |
| `66b720e8..1b913883` | 8 | ya-presente | pipeline de evals | reemplazado por `renovatio-evals` e issue #173, con schemas y gates endurecidos |
| `103b45ab..ea2592c1` | 9 | ya-presente | #228 | wrappers, persistencia, historial y harness tienen sucesores revisados en shared/API/workbench |
| `f9886dcc..f14a2597` | 16 | descartar | #228 | implementa un intérprete COBOL alternativo dentro del IR; duplica semántica y no es la ruta canónica de equivalencia por ejecución de artifacts |
| `aff71b23..824dabeb` | 12 | ya-presente | #228 | comparación, ignored fields, fixtures y smoke tienen sucesores en el harness y laboratorio de equivalencia |
| `736b3b11` | 1 | ya-presente | AC-01 | `main` ya configura SQLite persistente en `renovatio-api` |
| `09654570..cab441b2` | 2 | ya-presente | workbench | topology/search/zoom fueron reemplazados por las entregas posteriores del canvas |
| `6ee45fcf..3ea483b9` | 5 | descartar | #225 | layout/naming local desacopla planner y emisor CICS; reproduce `TARGET_MANIFEST_MISMATCH` |
| `71178f44` | 1 | obsoleto | épica workbench cerrada | plan Theia superado por las entregas #174–#203 |

Total: **85**.

`workbench-bootstrap` contiene esas mismas series más `7feaf780` (`chore(agora): sync skills,
commands, and swarm 010-023 state`), clasificado **obsoleto**: importar un ledger histórico sobre el
ledger actual destruiría trazabilidad en vez de preservarla.

## Ramas locales adicionales

Se inspeccionaron todas las refs `workspace/*`; sólo las siguientes tienen commits a la izquierda
de `origin/main`. Las demás tienen cero commits propios.

| Ref | Raw | Sin equivalentes | Clasificación | Evidencia / destino |
|---|---:|---:|---|---|
| `issue-126-residual-semantic-enrichment` | 1/292 | 0/291 | ya-presente | patch exacto de `4936fbbe` en `03e6b0dd` |
| `issue-128-idiomatic-polish-proposals` | 5/247 | 3/245 | obsoleto | tres cierres/planes Agora de una revisión histórica; dos patches ya presentes |
| `issue-179-domain-model-editor` | 1/52 | 1/52 | obsoleto | sólo cierre governance; funcionalidad y publicación están en `main` |
| `issue-206-cycle2` | 3/2 | 3/2 | ya-presente | squash `87509804` (#237) contiene INITIALIZE y level-88 SET |
| `issue-207-complete` | 4/1 | 4/1 | ya-presente | squash `6b468651` (#238) contiene PERFORM completo y evidencia |
| `issue-207-perform-thru-varying-times` | 2/1 | 2/1 | ya-presente | subconjunto del squash #238 |
| `issue-215-unified-generation` | 4/3 | 4/3 | ya-presente | squash `a598d8d1` (#236) y normalización posterior |
| `renovatio-workbench` | 1/64 | 0/63 | ya-presente | patch-equivalent |
| `theia-ai-explainable-agents` | 1/45 | 0/44 | ya-presente | patch-equivalent |
| `theia-change-sets` | 1/44 | 0/43 | ya-presente | patch-equivalent |
| `worktree-agent-ad38cefc26401ecbd` | 2/116 | 2/116 | ya-presente | skeleton/tests F7 superados por la entrega JCL integrada y sus remediaciones |

## Ports y descartes de AC-01

- Commits funcionales portados: **0**.
- Commits de ramas históricas aplicados: **0**.
- Series con valor conservado en destinos posteriores: modelo/arquitectura en #225; equivalencia
  vertical en #228.
- Series descartadas: intérprete de source replay (16), MVC/naming regresivo (5).
- Series obsoletas: proyección de modelo paralela (14), plan Theia (1) y sync Agora workbench (1).

## Continuidad de la épica #205

#205 no quedó vacía ni fue reemplazada. Cerró #207, #215, #217 y #218, pero mantiene abiertos #206
y #208–#214, además de #216. La convergencia los consume de este modo:

| Backlog #205 | Programa #221 |
|---|---|
| #214 | AC-04/#225, AC-05/#226 y AC-06/#227 |
| #216 | AC-07/#228 como golden master E2E |
| #206 y #208–#213 | slices/prerrequisitos funcionales de AC-07 |
| #207, #215, #217, #218 | baseline integrada; sólo regresión |

Secuencia recomendada: cerrar AC-01; seguir con AC-02/#223 y AC-04/#225; después AC-05/#226 y
AC-06/#227; terminar con AC-07/#228. Para el E2E de `CBACT01C`, priorizar #213 y el residual de
#206. AC-03/#224 puede correr en paralelo una vez definida la licencia.

## Premisa corregida

El issue decía “85 commits exclusivos del checkout actual”. Al ejecutar AC-01, el checkout del
usuario ya estaba en #217 y resultó ser ancestro de `main` (`0/16`). Los 85 commits corresponden a
la línea F8 originalmente auditada; se conservaron como universo vinculante y se añadió el estado
real de #217. El checkout sucio del usuario no fue modificado: todo el trabajo se hizo desde un clon
aislado.
