# Plan de implementación · #222 — reconciliación de ramas y baseline canónica

## Premisas

- Ejecutar desde una rama nueva basada en `origin/main@6b468651` dentro de un clon aislado.
- Mantener intacto el checkout de trabajo del usuario y todas las referencias históricas.
- Tratar el merge de una rama completa como prohibido; sólo se permiten ports registrados en el
  inventario.
- No modificar el contrato MVC/CICS ni implementar gaps funcionales de la épica #205.

## Pasos

1. **Capturar el grafo de entrada.** Registrar HEADs, merge-bases, conteos raw y patch-equivalent,
   relación de ancestro, `range-diff`, patch IDs y paths afectados para todas las ramas con commits
   propios.
2. **Agrupar y clasificar.** Consolidar commits consecutivos por intención técnica. Para cada serie,
   asignar `portar`, `ya-presente`, `obsoleto` o `descartar`, owner, issue destino y fundamento.
3. **Seleccionar la baseline.** Mantener `origin/main`; aplicar exclusivamente las series marcadas
   para AC-01. Registrar de forma expresa si el conjunto de ports es vacío.
4. **Documentar decisiones.** Crear el informe de integración y ADR que fijen la estrategia de
   convergencia, el vínculo #205→#221/#228 y el procedimiento de rollback.
5. **Verificar.** Ejecutar las tres pruebas focalizadas del routing MVC/CICS, el reactor hasta éxito
   o primer fallo real, `git diff --check`, validación Agora y revisión de trazabilidad.
6. **Cerrar gobernadamente.** Registrar artefactos/evidencia, satisfacer criterios por etapas,
   solicitar aprobación del spec-owner, crear un Conventional Commit, publicar la rama y abrir PR
   contra `main` vinculada a #222.

## Evidencia esperada

| Criterio | Evidencia |
|---|---|
| `inventory` | informe con inventario completo y comandos reproducibles |
| `clean-integration` | base SHA, lista explícita de ports, `status` y `diff --check` |
| `mvc-contract` | resultado de las tres pruebas focalizadas y ADR vigente |
| `history-integrity` | merge-base, rev-list, range-diff, patch-id y mapa #205/#221 |
| `baseline-tests` | reporte del reactor con primer fallo real o resultado verde |
| `rollback` | SHAs de entrada y receta no destructiva para recrear ramas |

## Riesgos y controles

- **Falsos positivos de patch-equivalence:** confirmar por resultado funcional y paths, no sólo por
  `--cherry-pick`.
- **Series antiguas mezcladas con metadata Agora:** clasificar por intención y excluir ledger
  histórico de cualquier port selectivo.
- **Pérdida de commits sólo locales:** conservar nombres y SHAs del remote temporal `workspace` y
  documentar cómo materializarlos antes de retirar cualquier rama.
- **Fallo global preexistente de Agora:** distinguirlo de los artefactos nuevos de #222 y no
  reescribir evidencias históricas para obtener un verde artificial.

## Rollback

No hay reescritura de historia. Antes de integrar la PR, `main` continúa apuntando a `6b468651` y
las líneas de entrada siguen recuperables en `7feaf780`, `71178f44` y `2be7528e`. Ante un problema,
se revierte el commit/merge de #222 o se crea una rama nueva desde cualquiera de esos SHAs; nunca se
fuerza `main` hacia atrás.
