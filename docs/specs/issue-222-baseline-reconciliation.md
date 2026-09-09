# Especificación · #222 — reconciliación de ramas y baseline canónica

- **Swarm:** `architecture-convergence-2026` (048)
- **Work item:** `baseline-reconciliation`
- **Issue:** https://github.com/Modern-Ash/renovatio/issues/222
- **Método:** spec-driven
- **Baseline de entrada:** `origin/main` en `6b46865171a795a18e84c570211c1dea14a7e6ea`

## Objetivo

Establecer una única línea canónica posterior al merge de #207, explicar de forma reproducible qué
ocurrió con cada línea de trabajo divergente y preservar un camino de recuperación. La
reconciliación no es un merge masivo: cada serie exclusiva debe quedar clasificada como `portar`,
`ya-presente`, `obsoleto` o `descartar` antes de que pueda entrar en la baseline.

La entrega también deja explícita la relación entre la épica funcional histórica #205 y la épica
de convergencia #221. La segunda ordena e integra el trabajo; no reemplaza ni cancela los gaps
funcionales de la primera.

## Hechos de partida

| Referencia | SHA | Relación con `origin/main` |
|---|---|---|
| `workspace/agora/issue-217-carddemo-coverage` | `2be7528e2df2d2c2083036f891a44a66ff1ddbd6` | ancestro; `0` propios / `16` detrás |
| `workspace/agora/renovatio-workbench-bootstrap` | `7feaf7803db998c0c3fd87bf54a07396d78cce44` | `86/108` raw; `83/105` sin equivalentes de parche |
| `workspace/agora/decision-engine-f8` | `71178f4451fa2616036d5c3a3967da519993ef77` | `85/108` raw; `82/105` sin equivalentes de parche |

Las dos líneas materialmente divergentes comparten el merge-base
`c89d307c34fe16e885a0ba63591b974fbd00b870`; workbench-bootstrap añade un commit sobre F8. Estos
identificadores son referencias de rollback y no deben borrarse ni reescribirse durante AC-01.

## Contrato de reconciliación

1. El inventario se obtiene con `merge-base`, `rev-list --left-right`, `--cherry-pick`,
   `range-diff`, `patch-id` e inspección de paths. Debe poder repetirse con comandos documentados.
2. La unidad de decisión es una serie coherente de commits, no un branch completo.
3. Sólo una serie clasificada `portar`, con owner y justificación, puede aplicarse a la nueva rama.
4. `ya-presente` significa que el resultado funcional o de parche ya está en `main`, aunque el SHA
   sea diferente. `obsoleto` significa que el supuesto o infraestructura cambió. `descartar`
   significa que la serie no pertenece a la arquitectura objetivo o que volver a introducirla
   causaría regresión o duplicación.
5. Si una serie conserva valor pero pertenece a un issue posterior, su clasificación sigue siendo
   `portar`, pero el destino y owner quedan registrados; AC-01 no adelanta esa implementación.
6. No se permite merge ciego, rebase destructivo, `reset --hard`, force-push ni borrado de las
   referencias de entrada.
7. La baseline de salida debe contener únicamente documentación, automatización de auditoría y los
   ports aprobados explícitamente. Si el análisis concluye que no existe un port seguro para AC-01,
   cero ports es un resultado válido y verificable.

## Relación entre las épicas #205 y #221

La épica #205 mantiene el backlog de cobertura funcional COBOL. A la fecha de esta baseline están
cerrados #207, #215, #217 y #218; continúan abiertos #206 y #208–#214, además de #216. El checkbox
de #207 en el cuerpo de #205 está desactualizado respecto del estado real del issue.

La épica #221 es la secuencia de convergencia y utiliza ese backlog así:

| Trabajo histórico | Destino en convergencia | Decisión |
|---|---|---|
| #214, validación y hardening de build | AC-04 / AC-05 / AC-06 | entrada directa a calidad, CI y release |
| #216, golden master CardDemo | AC-07 / #228 | criterio E2E de salida |
| #206 y #208–#213 | slices funcionales previos o internos de AC-07 | completar antes de declarar equivalencia E2E |
| #207, #215, #217 y #218 | baseline ya integrada | preservar y proteger con regresión |

Orden recomendado después de AC-01: #223 y #225, luego pipeline/proveedores, y finalmente #228.
Para destrabar el E2E de `CBACT01C`, priorizar #213 y el residual de #206 antes de cerrar #228.
#224 puede avanzar en paralelo una vez resuelta la decisión de licencia.

## Aceptación

- Existe un inventario por series con conteos, SHAs, clasificación, owner, destino y justificación.
- La rama de entrega nace de `origin/main`; `git diff --check` queda limpio y no incorpora cambios
  del checkout del usuario.
- La decisión MVC/CICS canónica de `main` permanece sin cambios y sus tres pruebas focalizadas
  continúan verdes.
- El reactor se ejecuta hasta éxito o hasta el primer fallo real, que queda atribuido y documentado.
- El informe enumera toda serie portada y no portada, y ofrece comandos de recuperación basados en
  los SHAs de entrada sin reescribir historia.
- Los cinco artefactos Agora requeridos (`spec`, `implementation-plan`, `integration-report`,
  `decision-record`, `test-report`) quedan registrados y trazables.

## Fuera de alcance

- Implementar en AC-01 los gaps funcionales abiertos de #205.
- Resolver en bloque los trabajos AC-02 a AC-07.
- Borrar ramas históricas o modificar el checkout sucio del usuario.
- Cambiar el contrato MVC/CICS ya aceptado en `main`.
- Reparar hallazgos históricos del ledger Agora ajenos a #222.

## Preguntas resueltas

- **¿Quedó trabajo de #205?** Sí; nueve hijos siguen abiertos y quedan absorbidos como
  prerrequisitos/slices funcionales de la convergencia, no cancelados.
- **¿Conviene converger ahora?** Sí. La rama de #217 ya está integrada y la divergencia material
  está suficientemente identificada para fijar una baseline antes de sumar más features.
- **¿Se mergean las ramas completas?** No. Se conserva `main` y se seleccionan series por evidencia.
- **¿Qué ocurre si no hay ports seguros?** Se documenta cero ports en AC-01 y cada serie con valor
  se asigna a su issue destino; la decisión evita reintroducir deuda bajo apariencia de integración.
