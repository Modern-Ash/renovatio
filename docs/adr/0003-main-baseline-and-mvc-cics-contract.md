# ADR 0003: `main` como baseline y contrato MVC/CICS canónico

- **Estado:** aceptado
- **Fecha:** 2026-09-08
- **Issue:** #222 (AC-01)
- **Decide:** Spec Owner de `architecture-convergence-2026/baseline-reconciliation`

## Contexto

La línea histórica `decision-engine-f8` divergió de `main` en `c89d307c` y llegó a 85 commits
exclusivos. Cinco de esos commits activaron un layout MVC alternativo y convenciones configurables
que no estaban coordinadas con el planner/emisor CICS. En esa línea,
`JavaGenerationRegistryRoutingTest.cicsControllerIsPlannedBeforeManifestValidation` falla con
`TARGET_MANIFEST_MISMATCH`: el emisor produce `RoutedCicsController.java`, pero el manifest
planifica otra ruta/nomenclatura. La misma línea habilita `LAYERED_MVC` sin actualizar su contrato
de activación y deja rojo `ArchitectureTransformerTest.rejectsInactiveAndDuplicateProfiles`.

`main@6b468651` contiene implementaciones posteriores y revisadas para manifest canónico, package
roots y layouts. Mezclar los cinco commits antiguos reintroduciría el desacople que AC-01 debe
eliminar.

## Decisión

1. La baseline canónica es `origin/main@6b46865171a795a18e84c570211c1dea14a7e6ea`, posterior a
   los merges de #215, #206 y #207.
2. Preview y apply consumen el mismo `ArtifactManifest`; los artifacts CICS se planifican antes de
   validar la salida del emisor.
3. El contrato de naming preserva el sufijo explícito `CicsController` y aplica package roots a
   través de `ArchitectureLayoutOverrides`. No se restaura la implementación local paralela de
   `java.layout.*` de la rama F8.
4. `TRANSACTION_SCRIPT` conserva rutas legacy; `HEXAGONAL` conserva sus capas bajo
   `modules/<módulo>/...`; `LAYERED_MVC` se representa mediante el perfil canónico de `main` y sus
   overrides, sin crear un segundo `ArchitectureModel`.
5. Los tres guardrails focalizados de esta decisión son:
   - `ArchitectureTransformerTest.supportsLayeredProfilesAndRejectsDuplicateProfiles`;
   - `JavaArchitectureLayoutPlannerTest.plansConditionalCicsControllerInBothLayouts`;
   - `JavaGenerationRegistryRoutingTest.cicsControllerIsPlannedBeforeManifestValidation`.
6. Los cinco commits antiguos `6ee45fcf..3ea483b9` se clasifican `descartar` para AC-01. Cualquier
   cambio futuro de modelo/layout debe entrar por #225 con spec nueva y estos guardrails verdes.

## Consecuencias

- AC-01 no porta código funcional: mantiene el árbol probado de `main` y añade trazabilidad,
  automatización de auditoría y referencias de rollback.
- #225 puede unificar `DomainModel`, `ArchitectureModel` y `ArtifactManifest` sin heredar un modelo
  paralelo no revisado.
- #228 recibe un contrato estable para medir la vertical COBOL→Java y no una mezcla de planners.
- Cambiar nombres o rutas CICS requerirá actualizar planner, manifest, emisor, preview y los tres
  guardrails en una única entrega.

## Alternativas descartadas

- **Merge completo de F8/workbench:** duplica implementaciones ya presentes y reintroduce los
  fallos descriptos.
- **Cherry-pick de los cinco commits MVC:** conserva el desacople de nombres/rutas.
- **Rebase o force-push de las líneas viejas:** reduce auditabilidad y contradice el rollback
  requerido por #222.

## Recuperación

Las referencias remotas `ac01-pre-convergence-main-20260908`,
`ac01-pre-convergence-f8-20260908` y `ac01-pre-convergence-workbench-20260908` preservan los tres
estados de entrada. Revertir AC-01 consiste en revertir su merge; inspeccionar o recuperar una
línea histórica consiste en crear una rama nueva desde el tag correspondiente. Ningún paso requiere
reescribir `main`.
