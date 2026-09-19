---
schema: "agora/work/v1"
id: "fix-copybook-index-shadowing"
swarm: "llm-domain-entities-epic"
title: "Fix: bug de shadowing en el \u00edndice de copybooks de SimpleCobolIrParser"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"test-reproduces-bug":"Test unitario nuevo reproduce el shadowing y verifica el fix","xref-fields-resolved":"XREF-ACCT-ID y dem\u00e1s campos de CVACT03Y.cpy aparecen al parsear CBTRN02C.cbl real","no-regressions":"mvn -pl renovatio-cobol-ir -am test -Djacoco.skip=true pasa sin regresiones"}
satisfied-criteria: []
criterion-statuses: {"test-reproduces-bug":[],"xref-fields-resolved":[],"no-regressions":[]}
required-artifacts: []
child-work-refs: []
budget-limits: null
parent-work: "llm-domain-entities-epic/epic-llm-domain-entities"
---

# Fix: bug de shadowing en el índice de copybooks de SimpleCobolIrParser

## Description

Parte de epic-llm-domain-entities.

## Objetivo
Corregir el bug de shadowing en la indexación de copybooks de `SimpleCobolIrParser`, que hace que archivos sin extensión con el mismo nombre base que un copybook (ej. `scripts/markers/CVACT03Y` en el workspace de AWS CardDemo) tapen al copybook real (`app/cpy/CVACT03Y.cpy`) según el orden no determinista de `Files.walk`.

## Contexto técnico necesario
- Archivo: `renovatio-cobol-ir/src/main/java/org/modernash/renovatio/cobol/ir/parser/SimpleCobolIrParser.java`.
- Métodos involucrados: `parse(Path)`, `expandCopyStatements(...)`, `copybookIndex(Path)`, `workspaceRoot(Path)`, `buildCopybookIndex(Path)` (agregados en esta misma sesión de trabajo para implementar expansión de COPY, previamente inexistente).
- El bug: `buildCopybookIndex` usa `Files.walk(root, 12)` y para cada archivo regular calcula `baseName` = nombre sin extensión en mayúsculas, y hace `index.putIfAbsent(baseName, path)` — es decir, el PRIMER archivo encontrado con ese nombre base "gana", sin importar si tiene extensión de copybook real (`.cpy`, `.CPY`) o es un archivo sin extensión de otro propósito (en CardDemo, `scripts/markers/CVACT03Y` es un archivo marcador vacío de un script de build/CI, no un copybook).
- Reproducido y confirmado manualmente esta sesión: para `CBTRN02C.cbl` (que hace `COPY CVACT03Y.` en línea 112, definiendo `CARD-XREF-RECORD` con el campo `XREF-ACCT-ID`), el índice resolvía `CVACT03Y` → `/home/faguero/.renovatio/workspaces/aws-mainframe-modernization-carddemo/scripts/markers/CVACT03Y` en vez de → `.../app/cpy/CVACT03Y.cpy`. Esto significa que `XREF-ACCT-ID` nunca se agrega a `model.getDataItems()`, y por lo tanto ninguna inferencia de relación (ni la basada en nombre de propiedad, ni la basada en `FieldFlow`/MOVE) puede usarlo — bloquea directamente el caso de uso central de este epic.
- Se puede reproducir con: parsear `CBTRN02C.cbl` con `SimpleCobolIrParser` y verificar que `XREF-ACCT-ID` NO aparece en `model.getDataItems()`. El workspace de prueba real está en `/home/faguero/.renovatio/workspaces/aws-mainframe-modernization-carddemo/` (proyecto "AWS CardDemo" ya cargado en el backend Renovatio local, project_id `873d7a49-b178-414c-a10c-f5364ff37f3f` en `renovatio-api/data/renovatio.db`, tabla `projects`).

## Tareas
1. Decidir y documentar la estrategia de desambiguación al indexar copybooks: opciones razonables son (a) preferir explícitamente extensiones típicas de copybook (`.cpy`, `.CPY`, `.cbl`, `.CBL`, `.cob`, `.COB`) sobre archivos sin extensión al construir el índice — es decir, si hay colisión de `baseName`, que gane el candidato con extensión reconocida; o (b) permitir configurar/derivar los directorios de copybooks del proyecto en vez de indexar el árbol completo sin filtrar. Preferir (a) por ser genérica y no requerir configuración nueva por proyecto (consistente con el enfoque "sin hardcodear" pedido explícitamente por el usuario en esta sesión).
2. Implementar el fix en `buildCopybookIndex` (o el método que se decida) de forma que la resolución sea determinista y no dependa del orden de `Files.walk`.
3. Agregar un test unitario a `renovatio-cobol-ir` que reproduzca el escenario exacto: dos archivos con el mismo nombre base en directorios distintos, uno con extensión de copybook y otro sin extensión, y verificar que se resuelve el que tiene extensión de copybook. Puede usarse `@TempDir` de JUnit para crear la estructura de archivos sintética sin depender del workspace real de CardDemo.
4. Verificar contra el caso real: parsear `CBTRN02C.cbl` del workspace de CardDemo y confirmar que `XREF-ACCT-ID` (y el resto de los campos de `CVACT03Y.cpy`: `XREF-CARD-NUM`, `XREF-CUST-ID`, `FILLER`) ahora sí aparecen en `model.getDataItems()`.
5. Ejecutar la suite completa de `renovatio-cobol-ir` (`mvn -pl renovatio-cobol-ir -am test -Djacoco.skip=true`) y confirmar que no se rompió nada existente.

## Criterios de aceptación
- [ ] Test unitario nuevo reproduce el bug de shadowing y falla en el código viejo / pasa con el fix.
- [ ] `XREF-ACCT-ID` y los demás campos de `CVACT03Y.cpy` aparecen en `model.getDataItems()` al parsear `CBTRN02C.cbl` del workspace real de CardDemo.
- [ ] `mvn -pl renovatio-cobol-ir -am test -Djacoco.skip=true` pasa sin regresiones.
- [ ] La resolución de copybooks sigue funcionando sin necesidad de configuración adicional por proyecto (nada hardcodeado a la estructura de carpetas de CardDemo específicamente).

## Evidencia a dejar en el PR
- Output del test nuevo.
- Output de `mvn -pl renovatio-cobol-ir -am test -Djacoco.skip=true`.
- Verificación manual (script o test de integración) mostrando `XREF-ACCT-ID` presente en los data items de `CBTRN02C.cbl` parseado.

## Acceptance criteria

- [ ] **test-reproduces-bug:** Test unitario nuevo reproduce el shadowing y verifica el fix; stages: none
- [ ] **xref-fields-resolved:** XREF-ACCT-ID y demás campos de CVACT03Y.cpy aparecen al parsear CBTRN02C.cbl real; stages: none
- [ ] **no-regressions:** mvn -pl renovatio-cobol-ir -am test -Djacoco.skip=true pasa sin regresiones; stages: none

## Required artifacts

- none
