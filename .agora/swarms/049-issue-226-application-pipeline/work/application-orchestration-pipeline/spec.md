# AC-05 — Pipeline único de aplicación

## Resultado esperado

Renovatio dispone de una única frontera de aplicación, independiente de transportes y frameworks,
que coordina el ciclo completo desde el análisis hasta la aplicación de artefactos. API, CLI y MCP
adaptan sus entradas y salidas a esa frontera sin reproducir decisiones de migración.

## Alcance

- Casos de uso públicos para crear y analizar proyectos, revisar el modelo de dominio, resolver
  decisiones, planificar, previsualizar, validar, aplicar y exportar evidencia.
- Contratos mínimos para análisis de fuente, propuestas, proyección arquitectónica, emisión,
  refinamiento, validación y repositorios de proyectos y artefactos.
- Un snapshot inmutable de entrada y un manifest canónico compartido por preview, validate y apply.
- Precondiciones de source hash y manifest hash verificadas antes de cualquier escritura.
- Idempotencia observable para comandos mutables.
- Apply atómico: éxito completo o restauración del estado previo, con ChangeSet auditable.
- Un test kit reutilizable para contratos, determinismo, ausencia de side effects y paridad
  preview/apply.

## Fuera de alcance

- Reescribir parsers, Semantic IR, DomainModel, emitters o refinadores existentes.
- Incluir detalles HTTP, CLI, MCP, Spring, persistencia o filesystem en el modelo de aplicación.
- Cambiar el formato o la semántica de un target por conveniencia del orquestador.
- Microservicios, colas distribuidas o coordinación remota de transacciones.
- Completar AC-06 o retirar providers legacy dentro de este ciclo; AC-05 deja la frontera preparada.

## Contratos de comportamiento

### Queries sin efectos laterales

`plan` y `preview` operan sobre un snapshot identificado y no escriben en el workspace fuente, Git ni
repositorios de artefactos finales. Repetir la misma operación con entradas equivalentes produce el
mismo manifest y el mismo resultado ordenado.

### Validación

`validate` consume el mismo snapshot y manifest producidos por preview. Ejecuta gates configurados y
devuelve evidencia estructurada; no convierte un manifest obsoleto en uno nuevo silenciosamente.

### Aplicación

`apply` exige identidad de proyecto, idempotency key, source hash esperado y manifest hash esperado.
Rechaza de forma accionable entradas obsoletas antes de escribir. Una repetición con la misma clave y
payload devuelve el resultado previo; reutilizar la clave con otro payload falla.

La escritura materializa un ChangeSet con manifest, preimage y postimage. Ante un fallo parcial,
restaura el preimage y no publica un ChangeSet exitoso ni deja artefactos parciales.

### Adaptadores

API, CLI y MCP sólo autentican/autorizan, validan forma de entrada, invocan un caso de uso y traducen
el resultado. Las decisiones de perfil, pipeline, stale state, emisión, rollback y códigos de dominio
pertenecen a la frontera de aplicación.

## Errores mínimos y accionables

- Snapshot de fuente obsoleto.
- Manifest obsoleto o desconocido.
- Idempotency key reutilizada con un payload diferente.
- Gate de validación fallido.
- Capability/port no disponible.
- Apply revertido después de un fallo de escritura.

Los errores deben ser tipados en application y conservar una traducción estable en cada transporte.

## Criterios verificables

1. `use-cases`: existen los nueve casos de uso públicos indicados por #226 y sus contratos no dependen
   de un transporte.
2. `ports`: los siete puertos funcionales y los repositorios requeridos son explícitos, mínimos y
   sustituibles en tests.
3. `side-effects`: todos los efectos externos atraviesan puertos; pruebas demuestran que plan y
   preview no escriben.
4. `idempotency`: pruebas cubren replay válido, conflicto de clave y rechazo de snapshot/manifest
   obsoleto.
5. `atomicity`: pruebas de fault injection prueban commit completo y rollback sin residuos, con
   ChangeSet que contiene preimage, postimage y manifest.
6. `thin-adapters`: pruebas arquitectónicas y de contrato demuestran que API, CLI y MCP invocan la
   misma frontera.
7. `contract-tests`: un kit reutilizable cubre puertos, paridad preview/apply y determinismo.

## Evidencia de cierre

- Tests unitarios sin Spring ni filesystem real para los casos de uso.
- Contract tests ejecutados contra adaptadores relevantes.
- Pruebas de idempotencia, stale state, fault injection y rollback.
- Prueba de workspace inmutable después de plan y preview.
- Prueba de identidad del manifest desde preview hasta apply.
- Comando, toolchain, commit exacto, resultado y artefactos registrados en Agora.

## Riesgos

- Extraer demasiado en un solo cambio puede producir una reescritura accidental; la compatibilidad se
  conserva mediante adaptadores incrementales.
- Una atomicidad limitada al filesystem no cubre Git o persistencia; el plan debe definir claramente
  el límite transaccional y las compensaciones comprobables.
- Mantener rutas productivas paralelas después de introducir application perpetuaría la autoridad
  duplicada; las superficies migradas deben tener una única delegación verificable.

## Dependencias

- AC-04 / #225 está mergeado y su modelo canónico es la autoridad de manifest.
- AC-02 / #223 aporta build y CI reproducibles.
- Los cambios que crucen COBOL→target deberán ejecutar el characterization guardrail.

## Decisiones de aclaración resueltas

1. El límite atómico cubre el conjunto de artefactos del workspace y el registro durable de
   ChangeSet/idempotencia. Todo se prepara antes de publicar; un fallo restaura el preimage. Git local
   sólo ocurre después de materializar y validar y debe poder compensarse. Red y pushes remotos no
   forman parte de `apply` en AC-05.
2. AC-05 migra las rutas productivas de plan, preview, validate y apply de API, CLI y MCP. AC-09
   conserva la unificación completa de catálogo de capabilities, discovery, UX y demás operaciones.
3. La orquestación legacy sólo puede sobrevivir como adaptador temporal detrás de los nuevos puertos.
   No habrá dual routing productivo para una misma operación; el retiro restante pertenece a AC-06.
4. La idempotency key se identifica por proyecto, caso de uso y clave, y persiste el digest canónico
   del comando y su resultado terminal. Misma clave y digest reproduce el resultado sin efectos;
   misma clave con otro digest falla. Los intentos revertidos nunca se presentan como exitosos.
5. Los nueve casos de uso deben existir como contratos públicos. Plan, preview, validate y apply
   quedan conectados end-to-end; los demás pueden adaptar servicios actuales, pero requieren tests de
   contrato y no pueden dejar decisiones de orquestación en los transportes.
