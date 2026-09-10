# Especificación · #215 — Pipeline canónico de generación COBOL→Java

- **Swarm:** `issue-215-unified-generation` (045)
- **Work item:** `unified-generation-pipeline`
- **Issue:** https://github.com/Modern-Ash/renovatio/issues/215
- **Método:** spec-driven

## Problema

Renovatio expone generación directa, `plan`/`apply`, CLI y API, pero la traducción de los cuerpos
Java no tiene hoy un contrato productivo único. El generador crea primero cuerpos con
`// TODO: Implement COBOL business logic` y después intenta reemplazarlos mediante OpenRewrite.
La salida puede quedar como stub si esa etapa no se aplica, mientras el harness de caracterización
ejercita el traductor directamente. Además, el CLI presupone un directorio
`generated-java-stubs` en vez de consumir la ubicación informada por el motor.

## Objetivo

Hacer que toda generación Java productiva —directa, `plan`/`apply`, CLI y API— use una sola cadena
para construir el cuerpo traducido desde el IR COBOL. La presencia de un sidecar anotado puede
enriquecer el resultado, pero nunca decide si la lógica base se traduce.

## Requisitos

- **R1 — Traducción obligatoria.** Para cada programa con IR base válido, el cuerpo del servicio
  pasa exactamente una vez por el traductor semántico antes de publicarse. Un sidecar válido añade
  metadatos opcionales; su ausencia no deja un `TODO` para lógica soportada.
- **R2 — Fallo visible.** Si la traducción no puede reemplazar el placeholder de lógica, la
  generación falla de forma explícita. No se permite devolver éxito con
  `// TODO: Implement COBOL business logic`.
- **R3 — Acciones manuales.** Cada sentencia COBOL representada como `UNTRANSLATED` produce un
  `ManualActionItem` estable con programa, archivo fuente, texto COBOL y motivo. El mismo conjunto
  se escribe una vez en el reporte canónico; el comentario visible en Java puede conservarse.
- **R4 — CLI real.** `renovatio plan` seguido de
  `renovatio apply <id> --no-dry-run --out <dir>` escribe en `<dir>` los artefactos informados por
  el motor, sin depender de un nombre de directorio interno. Para un fixture completamente
  soportado, el `ServiceImpl` contiene la lógica traducida y ningún `TODO`.
- **R5 — Equivalencia de entradas.** Generación directa y `plan`/`apply` reciben el mismo perfil
  efectivo y producen el mismo mapa de artefactos para el mismo workspace. La capa CLI sólo copia
  esos artefactos; no los vuelve a generar ni los transforma.
- **R6 — API/proveedor.** La prueba de ciclo de aplicación verifica archivos y contenido generado,
  además del estado del job o plan.
- **R7 — Arquitectura documentada.** Un ADR declara la cadena canónica
  `parse → IR → semantic IR → arquitectura → TargetEmitter → traducción OpenRewrite → escritura`,
  delimita los componentes legacy todavía internos y prohíbe nuevos caminos paralelos.
- **R8 — Regresión.** Permanecen verdes provider-cobol, OpenRewrite, CLI, API y la caracterización
  offline. No cambia la semántica de verbos ya soportados ni el formato de perfiles.

## Criterios del work item

| Criterio | Requisitos |
| --- | --- |
| `canonical-body-path` | R1, R2 |
| `cli-plan-apply` | R4, R5 |
| `api-lifecycle` | R6 |
| `manual-actions` | R3 |
| `architecture-record` | R7 |
| `regression-green` | R8 |

## Fuera de alcance

- Añadir verbos nuevos: continúa en #206.
- Implementar File I/O/VSAM o `CALL`: #213.
- Cambiar layouts MVC/Hexagonal/Clean: #214.
- Ejecutar el golden-master de un programa CardDemo completo: #216, desbloqueado por este trabajo.
- Renombrar inmediatamente todos los directorios históricos `generated-*-stubs`; se documentan
  como compatibilidad y no se usan como contrato entre motor y CLI.

## Decisiones de producto

- `JavaGenerationService` sigue siendo el orquestador COBOL→Java mientras exista el adaptador
  legacy; `TargetEmitter` continúa siendo la frontera por lenguaje.
- El mapa de artefactos devuelto por el motor es la fuente de verdad para CLI/API.
- Un resultado con placeholder de lógica es fallo, no éxito parcial.

## Ambigüedades

No quedan decisiones materiales abiertas. El cambio preserva formatos y perfiles existentes y
limita la consolidación al camino Java requerido por #215.
