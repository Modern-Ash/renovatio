# Spec · Alinear el manifiesto de arquitectura entre preview y generación (#218)

- **Swarm:** `issue-218-manifest-consistency` (042)
- **Work item:** `manifest-consistency-fix`
- **Issue:** https://github.com/Modern-Ash/renovatio/issues/218
- **Método:** spec-driven

## Contexto

`JavaGenerationService` expone dos operaciones que deben describir el **mismo** conjunto de
artefactos Java para un mismo workspace + perfil efectivo:

- `previewArchitecture(query, workspace[, effective])` → `ArchitectureResult`, cuyo
  `manifest().artifacts()` enumera las rutas planificadas.
- `generateInterfaceStubs(query, workspace[, effective])` → `StubResult`, cuyo
  `getGeneratedCode().keySet()` enumera las rutas realmente emitidas. Antes de devolver,
  `emitProjected(...)` invoca `applyManifest(targetModel, emitted)`, que exige que cada archivo
  emitido (por nombre) esté en `targetModel.targetStructure().artifactPaths()`, si no lanza
  `TargetManifestMismatchException` con código `TARGET_MANIFEST_MISMATCH`.

Tras el merge de `44583341` ("fix(workbench): align architecture canvas preview layout") en `main`,
dos rutas de cálculo del manifiesto quedaron desalineadas. `JavaGenerationRegistryRoutingTest`
falla en `main` (commit `5835fdeb`) con 2 tests en rojo:

1. **`cicsControllerIsPlannedBeforeManifestValidation`** (línea 130)
   - Programa COBOL con `EXEC CICS LINK PROGRAM('BACKEND') END-EXEC.`
   - El preview planifica `RoutedCicsController.java`.
   - `generateInterfaceStubs` falla: `TARGET_MANIFEST_MISMATCH: unexpected emitted path RoutedCicsController.java`
     → el manifiesto que usa `applyManifest` no contiene el controller que el generador legacy sí emite.

2. **`architectureCanvasPackageRootsFeedPreviewAndGeneration`** (línea 172)
   - Perfil con package roots custom del Architecture Canvas
     (`architecture.package.*` en `MigrationProfile.extensions()`, interpretados por
     `ArchitectureLayoutOverrides`).
   - `preview` espera `com/acme/domain/RoutedDTO.java`, `com/acme/services/RoutedService.java`,
     `com/acme/services/RoutedServiceImpl.java`.
   - `generateInterfaceStubs` emite `modules/routed/model/RoutedDTO.java`,
     `modules/routed/service/RoutedService.java`, `modules/routed/service/RoutedServiceImpl.java`
     → ignora los package roots y aplica el prefijo `modules/<módulo>/` con nombres de capa por
     defecto.

## Objetivo

`previewArchitecture` y `generateInterfaceStubs` producen manifiestos **idénticos** (mismas rutas,
mismo orden) para todo workspace + perfil efectivo, incluyendo:

- programas con comandos `EXEC CICS` que planifican un controller,
- perfiles con package roots / sufijos / class names custom del Architecture Canvas.

## Requisitos (SHALL)

- **R1 — Paridad CICS.** Para un programa con `EXEC CICS`, el conjunto de rutas de
  `previewArchitecture(...).manifest().artifacts()` es exactamente igual al de
  `generateInterfaceStubs(...).getGeneratedCode().keySet()`. Ambos contienen la ruta del
  controller CICS. `generateInterfaceStubs` no lanza `TARGET_MANIFEST_MISMATCH` ni devuelve
  `StubResult` fallido para este caso.

- **R2 — Paridad de package roots del canvas.** Cuando `MigrationProfile.extensions()` define
  `architecture.package.<layer>` (y/o `architecture.suffix.*`, `architecture.class.*`), tanto el
  preview como la generación emiten las rutas bajo esos roots (p.ej. `com/acme/domain/…`,
  `com/acme/services/…`) y **no** el prefijo `modules/<módulo>/` con nombres por defecto. El
  contenido generado usa el `package` y los `import` coherentes con esos roots.

- **R3 — Fuente única de verdad.** El manifiesto que valida `applyManifest`
  (`targetModel.targetStructure().artifactPaths()`) y el que devuelve `previewArchitecture`
  (`manifest().artifacts()`) se derivan del mismo cálculo (mismo planner + mismos overrides), de
  modo que no puedan volver a divergir sin romper un test.

- **R4 — Sin regresión.** `mvn -o test -pl renovatio-provider-cobol -Dexec.skip=true` pasa al
  100% (incluye `JavaGenerationRegistryRoutingTest`, `JavaArchitectureLayoutPlannerTest`,
  `JavaArchitectureSourceLayoutTest`). No se agregan `@Disabled` ni `assumeTrue` nuevos. Los
  módulos `renovatio-architecture` y `renovatio-provider-java` siguen verdes.

- **R5 — Causa raíz documentada.** El `verification-report` identifica qué componente calculaba
  el manifiesto de forma distinta en cada camino y por qué el merge `44583341` lo introdujo.

## Criterios de aceptación (mapa a work item)

| Criterio | Requisito |
| --- | --- |
| `cics-controller-parity` | R1, R3 |
| `canvas-package-roots-parity` | R2, R3 |
| `regression-green` | R4 |
| `root-cause-documented` | R5 |

## Fuera de alcance

- Implementar la traducción real de `EXEC CICS` a código (issue #211).
- Cambiar la semántica de layout de ningún estilo de arquitectura más allá de lo necesario para
  la paridad.
- Node / Python layout planners (solo se tocan si comparten el defecto y R4 lo exige).

## No ambigüedades pendientes

Ninguna. El comportamiento esperado está fijado por los dos tests ya presentes en el repo
(`JavaGenerationRegistryRoutingTest.cicsControllerIsPlannedBeforeManifestValidation` y
`architectureCanvasPackageRootsFeedPreviewAndGeneration`); esta spec solo formaliza y generaliza
su intención.
