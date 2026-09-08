# Spec · Reporte de cobertura del pipeline sobre el corpus CardDemo (#217)

- **Swarm:** `issue-217-carddemo-coverage` (043)
- **Work item:** `carddemo-coverage-report`
- **Issue:** https://github.com/Modern-Ash/renovatio/issues/217
- **Método:** spec-driven

## Contexto

Renovatio nunca corrió su pipeline COBOL→Java de punta a punta sobre un programa real. El único
test que toca el CardDemo (`CobolParsingServiceDemoWorkspaceTest`) sólo cuenta archivos y usa un
path absoluto externo con `assumeTrue`. El propio contrato de caracterización declara que sólo
**2 de 13** fixtures se traducen end-to-end hoy
(`CharacterizationFixtureContractTest.SUPPORTED = {"move-numeric", "data-intent-redefines"}`).

Falta una medición objetiva: sobre un corpus real y completo, ¿qué porcentaje parsea, genera Java
y compila, y cuáles son los constructos que más bloquean?

## Objetivo

Una herramienta reproducible que corre el pipeline sobre **todo** el COBOL del AWS CardDemo y
publica un reporte agregado (Markdown legible + JSON versionado para seguir la tendencia), sin
modificar el motor de traducción.

## Corpus

El AWS Mainframe Modernization CardDemo (Apache-2.0, © Amazon.com Inc.). Se vendoriza en el repo
el subconjunto de **fuentes** (no infra, no imágenes, no docs):

- `app/cbl/` — 31 programas (batch + online CICS), ~20.650 líneas
- `app/cpy/`, `app/cpy-bms/` — copybooks y mapas BMS
- `app/jcl/` — 85 JCL
- `app/app-transaction-type-db2/{cbl,cpy,cpy-bms,jcl}` — 3 programas DB2
- `app/app-authorization-ims-db2-mq/{cbl,cpy,cpy-bms,jcl}` — 8 programas IMS+DB2+MQ
- `app/app-vsam-mq/cbl` — 2 programas VSAM+MQ
- `LICENSE`, `NOTICE`

Ubicación: `renovatio-provider-cobol/src/test/resources/corpus/carddemo/` (o módulo/carpeta
equivalente que el plan justifique). Se añade una nota de procedencia
(`corpus/carddemo/PROVENANCE.md`: repo origen, commit, licencia).

## Requisitos (SHALL)

- **R1 — Corpus versionado.** Las fuentes COBOL/copybook/JCL del CardDemo quedan committeadas en
  el repo bajo una ruta de test dedicada, con `LICENSE`, `NOTICE` y `PROVENANCE.md`. Ningún
  binario ni asset no-fuente.

- **R2 — Herramienta de cobertura.** Existe una herramienta ejecutable (test JUnit con
  `@Tag("coverage")` o clase `main` en `renovatio-cli`/módulo de tooling) que, por cada programa
  COBOL del corpus, registra:
  - parseo: ok / ko (+ mensaje de error),
  - proyección a IR / semantic IR: ok / ko,
  - emisión Java: ok / ko (+ nº de archivos, presencia de `// TODO: Implement COBOL business logic`
    y `// Unhandled COBOL statement`),
  - compilación `javac` del Java emitido: ok / ko (+ primeros errores),
  - `ManualActionItem`s producidos,
  - verbos / constructos COBOL presentes y cuáles quedaron sin traducir.

- **R3 — Reporte agregado.** Se genera `docs/reports/carddemo-coverage.md` con, como mínimo:
  - N/total programas que parsean, M/total que emiten Java, K/total cuyo Java compila,
  - top de verbos/constructos no soportados por frecuencia (con nº de programas afectados),
  - desglose por subsistema: batch puro / CICS online / DB2 / IMS+MQ / VSAM+MQ,
  - tabla por programa (líneas, parse, emit, compile, #action items).

- **R4 — JSON de seguimiento.** Se committea `docs/reports/carddemo-coverage.json` con la misma
  información en forma estructurada y **determinista** (orden estable, sin timestamps ni paths
  absolutos), para diffear entre corridas.

- **R5 — Candidatos E2E.** El reporte nombra explícitamente los 3 programas batch más simples
  (sin `EXEC CICS` ni `EXEC SQL`) con mejor cobertura, como candidatos para el E2E de #216.

- **R6 — Reproducible y CI.** Un comando documentado (en README o `docs/`) ejecuta el reporte.
  Corre sin rutas absolutas ni acceso a red. Se integra a CI (perfil Maven dedicado o job
  nightly); no rompe el build por defecto aunque la cobertura sea baja (es medición, no gate).

- **R7 — Sin cambio de motor.** El `git diff` no modifica la lógica del traductor COBOL→Java
  (`cobol-openrewrite-recipes/PopulateCobolProcessRecipe`, `JavaGenerationService` salvo exponer
  métricas ya calculadas). `renovatio-provider-cobol` sigue 114/114; `renovatio-architecture` y
  `renovatio-provider-java` verdes.

## Criterios de aceptación (mapa a work item)

| Criterio | Requisito |
| --- | --- |
| `corpus-vendored` | R1 |
| `coverage-tool` | R2 |
| `aggregate-report` | R3 |
| `tracking-json` | R4 |
| `e2e-candidates` | R5 |
| `reproducible-ci` | R6 |
| `no-engine-change` | R7 |

## Fuera de alcance

- Mejorar la cobertura (eso son #206–#214).
- Traducir JCL o BMS (sólo se cuentan / clasifican).
- Ejecutar el Java generado o comparar comportamiento (eso es #216).
- Bank-of-Z y cics-genapp (otros demos): se pueden añadir después; este issue es CardDemo.

## Decisiones tomadas

- **Ubicación del corpus:** `renovatio-provider-cobol/src/test/resources/corpus/carddemo/`.
  Justificación: la herramienta de cobertura vive naturalmente en ese módulo (usa
  `CobolParsingService`, `JavaGenerationService`); mantener corpus y consumidor juntos evita
  paths cross-módulo. El plan puede proponer un módulo `renovatio-corpus` si el peso lo amerita.
- **Forma de la herramienta:** test JUnit `@Tag("coverage")` desactivado del build por defecto
  (`-DexcludedGroups=coverage` o Surefire config), activable con `-Pcoverage` o
  `-Dgroups=coverage`. Escribe los dos ficheros de reporte a `docs/reports/` del repo.
- **Runtime de compilación:** `javax.tools.ToolProvider.getSystemJavaCompiler()` en memoria
  (mismo patrón que `JavaGenerationRegistryRoutingTest.transactionScriptAndHexagonalLayoutsCompile`).

## Definiciones operativas

### D1 — Inventario y denominador
- Programa COBOL = archivo con extensión `.cbl`, `.CBL`, `.cob` o `.COB` bajo cualquier directorio
  `.../cbl*/` del corpus vendorizado. Copybooks (`.cpy`), mapas BMS (`cpy-bms`) y JCL **no**
  entran al denominador (se cuentan aparte).
- La identidad del programa es su `PROGRAM-ID`. Si dos archivos declaran el mismo `PROGRAM-ID`,
  se cuenta una vez y se registra el conflicto en el reporte.
- Subrutinas utilitarias (`CSUTLDTC`, `CBSTM03B`, `COBSWAIT`, …) entran al denominador y se marcan
  `role=subprogram`.
- El denominador total y la lista de archivos incluidos/excluidos se imprime en el reporte
  (`inventory` en el JSON).

### D2 — Clasificación por subsistema (exclusiva, primer match gana)
1. `ims-mq` — el programa está bajo `app-authorization-ims-db2-mq/` **o** contiene `EXEC DLI` /
   llamadas IMS (`CBLTDLI`).
2. `db2` — contiene `EXEC SQL`.
3. `cics-online` — contiene `EXEC CICS`.
4. `vsam-mq` — contiene llamadas MQ (`MQOPEN`, `MQPUT`, `MQGET`) sin encajar en 1–3.
5. `batch` — ninguno de los anteriores.
El directorio de origen tiene prioridad sobre el contenido; dentro del contenido, el orden es el
listado (DLI → SQL → CICS → MQ).

### D3 — "Constructo no soportado"
Para un programa dado, un verbo/constructo se cuenta como **no soportado** si el pipeline emite
al menos una de estas señales para él:
- `// Unhandled COBOL statement` en el Java emitido,
- un `ManualActionItem` que lo nombra,
- la proyección a IR / semantic IR lo descarta.
La mera presencia léxica (grep del verbo en el fuente) se reporta en una columna aparte
(`present`) para dar contexto de frecuencia, pero el ranking de "no soportado" usa sólo las
señales del pipeline.

### D4 — Orden de candidatos E2E (#216)
Filtrar a `subsystem == batch` con parseo OK y emisión OK, luego ordenar por:
1. compila con `javac` (sí antes que no),
2. menor cantidad de `// Unhandled COBOL statement` + `// TODO: Implement COBOL business logic`,
3. menor cantidad de `ManualActionItem`,
4. menor LOC,
5. `PROGRAM-ID` ascendente (desempate determinista).
Los 3 primeros son los candidatos.

### D5 — Alcance de "no-engine-change" (R7)
Se permite en `JavaGenerationService` / servicios de `renovatio-provider-cobol`:
- añadir accesores o variantes **aditivas y sin efectos secundarios** que expongan métricas ya
  calculadas (mapa de `ManualActionItem`, diagnósticos de emisión),
- eliminar las líneas `System.out.println("DEBUG: …")` / `"Generated Service Implementation…"`
  (limpieza pura, sin cambio de comportamiento).
Queda **prohibido** tocar la lógica de traducción: `PopulateCobolProcessRecipe`,
`CobolSemanticTranspiler` (salvo firma aditiva), plantillas, reglas OpenRewrite, mapeo de tipos.

## No ambigüedades pendientes

Ninguna material tras D1–D5. El alcance es medición sobre un corpus fijo con salidas deterministas.
