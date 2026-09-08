# Plan de implementación · Manifiesto de arquitectura consistente preview ↔ generación (#218)

- **Spec:** `docs/specs/issue-218-manifest-consistency.md`
- **Swarm / work:** `issue-218-manifest-consistency` / `manifest-consistency-fix`
- **Rama:** `agora/issue-218-manifest-consistency` (worktree `/tmp/renovatio-215`, base `main` `5835fdeb`)

## Diagnóstico preliminar

El commit `44583341` ("fix(workbench): align architecture canvas preview layout") aterrizó en `main`
de forma parcial:

- **Añadió** `renovatio-architecture/.../ArchitectureLayoutOverrides.java` (interpreta
  `architecture.package.*`, `architecture.suffix.*`, `architecture.class.*` de
  `MigrationProfile.extensions()`).
- **Modificó** `JavaArchitectureSourceLayout.location(...)` para aceptar rutas que no empiezan por
  `modules/` y derivar el package del propio path.
- **Añadió** dos tests a `JavaGenerationRegistryRoutingTest`
  (`cicsControllerIsPlannedBeforeManifestValidation`,
  `architectureCanvasPackageRootsFeedPreviewAndGeneration`) que **fallan** en `main`.

Las dos rutas de cálculo del manifiesto:

| | Preview (`previewArchitecture`) | Generación (`generateInterfaceStubs`) |
| --- | --- | --- |
| entrada | `prepareArchitecture(...).architecture()` | `prepareArchitecture(...)` + `emitProjected(architected.targetModel(), ...)` |
| manifiesto | `ArchitectureResult.manifest().artifacts()` | `targetModel.targetStructure().artifactPaths()` validado en `applyManifest(...)` |

Hipótesis a confirmar en `implementing`:

1. **CICS:** el planner (`JavaArchitectureLayoutPlanner.plan`) agrega el `*Controller.java`
   cuando hay I/O `TRANSACTION`, y eso llega a `ArchitectureResult.manifest()` (preview) pero
   **no** a `TargetStructure.artifactPaths()` del `TargetModel` proyectado que consume
   `applyManifest` — o el path del controller no matchea la convención (`RoutedCicsController.java`
   sin prefijo `modules/…` vs el resto con prefijo).
2. **Package roots:** `ArchitectureLayoutOverrides` se aplica al construir el grafo/manifiesto del
   preview pero el `TargetStructure` / `JavaArchitectureSourceLayout` de la generación usa el
   layout por defecto (`modules/<módulo>/model|service`), ignorando los overrides del perfil.

## Enfoque

**Principio:** una sola función construye el manifiesto y ambos caminos la consumen (R3). No
duplicar la lógica de layout entre `ArchitectureResult.manifest()` y
`TargetStructure.artifactPaths()`.

### Paso 0 — Reproducir (TDD rojo)
- Ejecutar `mvn -o test -pl renovatio-provider-cobol -Dtest=JavaGenerationRegistryRoutingTest -Dexec.skip=true` y confirmar los 2 fallos.
- Añadir asserts de diagnóstico temporales (o un test unitario nuevo a nivel
  `JavaArchitectureLayoutPlannerTest` / `ArchitectureResult`) que capturen la divergencia exacta
  entre `manifest().artifacts()` y `targetStructure().artifactPaths()` para (a) input CICS y
  (b) input con `customPackageProfile()`.

### Paso 1 — Fuente única del manifiesto
- Identificar dónde se materializa `TargetStructure.artifactPaths()` para el `TargetModel`
  proyectado (`architecture(List.of(semantic), effective, false, ...)` en `emitProjected`).
- Hacer que derive del **mismo** `JavaArchitectureLayoutPlanner` (mismos `LayoutContext`,
  mismos `ArchitectureLayoutOverrides.from(profile)`) que alimenta `ArchitectureResult.manifest()`.
- Si hoy hay dos invocaciones del planner con contexto distinto (una con overrides, otra sin),
  unificar el `LayoutContext`.

### Paso 2 — CICS controller en ambos lados
- Asegurar que la detección de I/O `TRANSACTION` que dispara el `*Controller` ocurre **antes** de
  construir `TargetStructure`, no solo en el `ArchitectureResult` del preview.
- Normalizar el path del controller a la misma convención que el resto de artefactos del estilo
  (con/ sin prefijo `modules/<módulo>/`), para que `applyManifest` lo reconozca por nombre y
  `JavaArchitectureSourceLayout.location(...)` sepa derivar su package.

### Paso 3 — Package roots del canvas en la generación
- `emitProjected` / la construcción de `TargetStructure` debe aplicar
  `ArchitectureLayoutOverrides.from(effective.profile())` sobre las rutas: `packageRoot(layer)`,
  `suffix`, `className`.
- Verificar que `JavaArchitectureSourceLayout.align(...)` (ya tolera rutas sin `modules/` tras
  `44583341`) produce `package com.acme.services;` e `import com.acme.domain.RoutedDTO;` para el
  caso `customPackageProfile()`.

### Paso 4 — Orden estable
- Fijar el orden de las rutas del manifiesto (p.ej. DTO → contrato → impl → controller) y que
  `getGeneratedCode()` (LinkedHashMap) lo preserve, para cumplir la clarificación "mismo orden".

### Paso 5 — Regresión
- `mvn -o test -pl renovatio-provider-cobol -Dexec.skip=true`
- `mvn -o test -pl renovatio-architecture -Dexec.skip=true`
- `mvn -o test -pl renovatio-provider-java -Dexec.skip=true`
- Revisar `NodeArchitectureLayoutPlanner` (tocado por `44583341`): si comparte el defecto y algún
  test lo exige, aplicar el mismo arreglo; si no, dejar constancia en el verification-report.

### Paso 6 — Verification report
- `docs/reports/issue-218-manifest-consistency-verification.md`: causa raíz (qué componente
  divergía, por qué `44583341` lo introdujo), diff resumido, salidas de test, mapeo criterio→evidencia.

## Archivos candidatos a tocar

- `renovatio-provider-cobol/.../service/JavaGenerationService.java` (`emitProjected`, `applyManifest`, construcción de `TargetModel`/`TargetStructure`).
- `renovatio-provider-java/.../emission/JavaArchitectureLayoutPlanner.java`
- `renovatio-provider-java/.../emission/JavaArchitectureSourceLayout.java`
- `renovatio-architecture/.../ArchitectureLayoutOverrides.java` (si falta exponer algo)
- `renovatio-architecture/.../ArchitectureTransformer.java` / `ArtifactLayoutPlanner.java` (punto de unificación).

## Riesgos

- Cambiar la convención de paths puede afectar `hexagonalRouteUsesTheCanonicalLayeredManifest` y
  `previewUsesTheEmissionManifestWithoutWritingTheWorkspace` (mismo archivo de test) — deben
  seguir verdes sin editarlos.
- `JavaArchitectureSourceLayoutTest` fija packages esperados; ajustar solo si la spec lo exige.

## Commits (Conventional Commits)

- `test(architecture): pin preview↔generation manifest parity for CICS and canvas roots (#218)` (rojo)
- `fix(architecture): derive emission manifest from the preview layout planner (#218)`
- `docs(agora): verification report for issue #218`
